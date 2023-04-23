package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.OrderBook
import kaiex.model.OrderBookEntry
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.SortedMap
import java.util.TreeMap

/*
 *
 */
class DYDXOrderBookSocketEndpoint(private val symbol:String): DYDXSocketEndpoint<OrderBook> {

    @Serializable(with = MessageSerializer::class)
    sealed class Message {
        abstract val type: String
        abstract val connection_id: String
        abstract val message_id: Int
    }

    @Serializable
    data class Connected(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int
    ) : Message()

    @Serializable
    data class Subscribed(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int,
        val channel: String,
        val id: String,
        val contents: Contents
    ) : Message() {
        @Serializable
        data class Contents(val asks: List<Level>, val bids: List<Level>)

        @Serializable
        data class Level(val size: String, val price: String, val offset: String)
    }

    @Serializable
    data class ChannelData(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int,
        val id: String,
        val channel: String,
        val contents: Contents
    ) : Message() {
        @Serializable
        data class Contents(val offset: String, val bids: List<List<String>>, val asks: List<List<String>>)
    }

    object MessageSerializer :
        JsonContentPolymorphicSerializer<Message>(
            Message::class
        ) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Message> {
            return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
                "connected" -> Connected.serializer()
                "subscribed" -> Subscribed.serializer()
                "channel_data" -> ChannelData.serializer()
                else -> throw Exception("ERROR: No Serializer found. Serialization failed.")
            }
        }
    }

    private val log: org.slf4j.Logger = LoggerFactory.getLogger("${javaClass.simpleName} ($symbol)")
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private var socket: WebSocketSession? = null

    // local order book
    private var currentBids = TreeMap<Float, Subscribed.Level>(Comparator.reverseOrder())
    private var currentAsks = TreeMap<Float, Subscribed.Level>()

    private var lastSendTime = Instant.now()

    private var nextSequenceNumber:Int = 0

    override suspend fun connect(): Resource<Unit> {

        return try {
            socket = client.webSocketSession {
                url(DYDXSocketEndpoint.Endpoints.DYDXSocket.getURL())
            }
            if(socket?.isActive == true) {
                Resource.Success(Unit)
            } else Resource.Error("Couldn't establish a connection.")
        } catch(e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to connect")
        }
    }

    override suspend fun subscribe(): Resource<Unit> {
        return try {
            val d = JsonObject(mapOf("type" to JsonPrimitive("subscribe"),
                "channel" to JsonPrimitive("v3_orderbook"),
                "id" to JsonPrimitive(symbol), "includeOffsets" to JsonPrimitive("true")
            ))
            socket?.send(Frame.Text(d.toString()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to subscribe")
        }
    }

    override fun observeUpdates(): Flow<OrderBook> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.transform { it ->
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    when(val dydxOrderBookUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxOrderBookUpdate)
                        is Subscribed-> emit(onSubscribed(dydxOrderBookUpdate))
                        is ChannelData -> {
                            onChannelData(dydxOrderBookUpdate)
                            //if(Instant.now() > lastSendTime + Duration.ofSeconds(2)) {
                            val ob = createOrderBook()
                            emit(ob)
                            //    lastSendTime = Instant.now()
                            //}
                        }
                    }
                }
                ?: flow { }
        } catch(e: Exception) {
            e.printStackTrace()
            flow { }
        }
    }

    private fun onConnected(message: Connected) {
        log.info("Connected with connection id: ${message.connection_id}")
    }

    private fun onSubscribed(message: Subscribed):OrderBook {
        log.debug("Subscribed to $message")
        currentBids = TreeMap<Float, Subscribed.Level>(Comparator.reverseOrder())
        currentAsks = TreeMap<Float, Subscribed.Level>()
        message.contents.bids.forEach { level -> currentBids[level.price.toFloat()] = level }
        message.contents.asks.forEach { level -> currentAsks[level.price.toFloat()] = level }
        return createOrderBook()
    }

    private fun onChannelData(message: ChannelData) {
        log.debug("Received channel data for $message")
        addOrReplaceLevels(message.contents.offset, currentBids, message.contents.bids)
        addOrReplaceLevels(message.contents.offset, currentAsks, message.contents.asks)
    }

    private fun addOrReplaceLevels(offset: String, levels: SortedMap<Float, Subscribed.Level>, updates: List<List<String>>) {
        updates.forEach { update ->
            val price = update[0].toFloat()
            val strPrice = update[0]
            val size = update[1]
            if(!levels.containsKey(price) || (levels.containsKey(price) && offset > levels[price]!!.offset)) {
                levels[price] = Subscribed.Level(size, strPrice, offset)
            }
        }
    }

    private fun createOrderBook():OrderBook = OrderBook(
        nextSequenceNumber++,
        symbol,
        currentBids
            .filter { (_, value) -> value.size.toDouble() > 0.0 }
            .map { (_, value) -> OrderBookEntry(value.size.toFloat(), value.price.toFloat()) },
        currentAsks
            .filter { (_, value) -> value.size.toDouble() > 0.0 }
            .map { (_, value) -> OrderBookEntry(value.size.toFloat(), value.price.toFloat()) },
        Instant.now()
    )

    override suspend fun disconnect() {
        socket?.close()
    }
}