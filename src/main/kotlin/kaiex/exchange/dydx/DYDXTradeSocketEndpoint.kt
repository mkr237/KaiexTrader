package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.OrderSide
import kaiex.model.Trade
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

/*
 *
 */
class DYDXTradeSocketEndpoint(private val symbol:String): DYDXSocketEndpoint<Trade> {

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
    data class Trade(val side:String,
                     val size: String,
                     val price: String,
                     val createdAt: String,
                     val liquidation: Boolean)

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
        data class Contents(val trades: List<Trade>)
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
        data class Contents(val trades: List<Trade>)
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

    private val log: Logger = LoggerFactory.getLogger("${javaClass.simpleName} ($symbol)")
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
                "channel" to JsonPrimitive("v3_trades"),
                "id" to JsonPrimitive(symbol)
            ))
            socket?.send(Frame.Text(d.toString()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to subscribe")
        }
    }

    override fun observeUpdates(): Flow<kaiex.model.Trade> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.transform {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    when(val dydxTradeUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxTradeUpdate)
                        is Subscribed -> onSubscribed(dydxTradeUpdate).forEach{ trade -> emit(trade) }
                        is ChannelData -> onChannelData(dydxTradeUpdate).forEach{ trade -> emit(trade) }
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

    private fun onSubscribed(message: Subscribed): List<kaiex.model.Trade> {
        log.debug("Subscribed with $message")
        return convertTradeList(message.contents.trades, true)
    }

    private fun onChannelData(message: ChannelData): List<kaiex.model.Trade> {
        log.debug("Received channel data: $message")
        return convertTradeList(message.contents.trades, false)
    }

    private fun convertTradeList(tradesIn: List<Trade>, historical: Boolean): List<kaiex.model.Trade> {
        var trades = ArrayList<kaiex.model.Trade>()
        tradesIn.reversed().forEach { trade ->
            trades.add(Trade(symbol,
                OrderSide.valueOf(trade.side),
                trade.size.toFloat(),
                trade.price.toFloat(),
                Instant.parse(trade.createdAt),
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                trade.liquidation,
                historical))
        }
        return trades
    }

    override suspend fun disconnect() {
        socket?.close()
    }
}