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

/*
 *
 */
class DYDXOrderBookStream(private val symbol:String): DYDXSocket<OrderBook> {

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
        data class Contents(val asks: List<Order>, val bids: List<Order>)

        @Serializable
        data class Order(val size: String, val price: String)
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
            json()
        }
    }
    private var socket: WebSocketSession? = null

    override suspend fun connect(): Resource<Unit> {

        return try {
            socket = client.webSocketSession {
                url(DYDXSocket.Endpoints.DYDXSocket.url)
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
                "id" to JsonPrimitive(symbol)
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
                ?.transform {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    when(val dydxOrderBookUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxOrderBookUpdate)
                        is Subscribed-> emit(onSubscribed(dydxOrderBookUpdate))
                        is ChannelData -> emit(onChannelData(dydxOrderBookUpdate))
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

        var bids = ArrayList<OrderBookEntry>()
        message.contents.bids.forEach {
            bids.add(OrderBookEntry(it.size.toFloat(), it.price.toFloat()))
        }

        var asks = ArrayList<OrderBookEntry>()
        message.contents.asks.forEach {
            asks.add(OrderBookEntry(it.size.toFloat(), it.price.toFloat()))
        }

        return OrderBook(message.id, bids, asks, Instant.now())
    }

    private fun onChannelData(message: ChannelData):OrderBook {
        log.debug("Received channel data for $message")

        var bids = ArrayList<OrderBookEntry>()
        message.contents.bids.forEach {
            bids.add(OrderBookEntry(it[1].toFloat(), it[0].toFloat()))
        }

        var asks = ArrayList<OrderBookEntry>()
        message.contents.asks.forEach {
            asks.add(OrderBookEntry(it[1].toFloat(), it[0].toFloat()))
        }

        return OrderBook(message.id, bids, asks, Instant.now())
    }

    override suspend fun disconnect() {
        socket?.close()
    }
}