package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.*
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.List

class DYDXMarketsSocketEndpoint: DYDXSocketEndpoint<MarketInfo> {

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
        val contents: Contents
    ) : Message() {
        @Serializable
        data class Contents(val markets: Map<String, MarketInfo> = emptyMap())
    }

    @Serializable
    data class ChannelData(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int,
        val channel: String,
        val contents: Map<String, MarketInfo> = emptyMap()
    ) : Message()

    @Serializable
    data class MarketInfo(
        val market: String? = null,
        val status: String? = null,
        val baseAsset: String? = null,
        val quoteAsset: String? = null,
        val stepSize: String? = null,
        val tickSize: String? = null,
        val indexPrice: String? = null,
        val oraclePrice: String? = null,
        val priceChange24H: String? = null,
        val nextFundingRate: String? = null,
        val nextFundingAt: String? = null,
        val minOrderSize: String? = null,
        val type: String? = null,
        val initialMarginFraction: String? = null,
        val maintenanceMarginFraction: String? = null,
        val transferMarginFraction: String? = null,
        val volume24H: String? = null,
        val trades24H: String? = null,
        val openInterest: String? = null,
        val incrementalInitialMarginFraction: String? = null,
        val incrementalPositionSize: String? = null,
        val maxPositionSize: String? = null,
        val baselinePositionSize: String? = null,
        val assetResolution: String? = null,
        val syntheticAssetId: String? = null
    )

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

    //private val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    private val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    private val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    //private val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
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

    private var markets: MutableMap<String, kaiex.model.MarketInfo> = mutableMapOf()

    override suspend fun connect(): Resource<Unit> {
        return try {
            log.info(DYDXSocketEndpoint.Endpoints.DYDXSocket.getURL())
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

        val nowISO = getISOTime()

        return try {
            val d = JsonObject(mapOf("type" to JsonPrimitive("subscribe"),
                                     "channel" to JsonPrimitive("v3_markets")
            ))
            socket?.send(Frame.Text(d.toString()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to subscribe")
        }
    }

    override fun observeUpdates(): Flow<kaiex.model.MarketInfo> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.transform {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    when(val dydxMarketsUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxMarketsUpdate)
                        is Subscribed -> onSubscribed(dydxMarketsUpdate).forEach{ market -> emit(market) }
                        is ChannelData -> onChannelData(dydxMarketsUpdate).forEach{ market -> emit(market) }
                    }
                }
                ?: flow { }
        } catch(e: Exception) {
            e.printStackTrace()
            flow { }
        }
    }

    private fun onConnected(message: Connected) {
        log.info("Connected with connection_id: ${message.connection_id}")
    }

    private fun onSubscribed(message: Subscribed):List<kaiex.model.MarketInfo> {
        log.debug("Subscribed to $message")
        markets = mutableMapOf()
        message.contents.markets.forEach {
            val marketInfo = kaiex.model.MarketInfo(
                symbol = it.key,
                status = parseMarketStatus(it.value.status!!),
                indexPrice = it.value.indexPrice?.toFloat() ?: 0f,
                oraclePrice = it.value.oraclePrice?.toFloat() ?: 0f
            )
            markets[it.key] = marketInfo
        }

        return markets.values.toList()
    }

    fun parseMarketStatus(statusStr: String): MarketStatus {
        return try {
            MarketStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            MarketStatus.OFFLINE
        }
    }

    private fun onChannelData(message: ChannelData):List<kaiex.model.MarketInfo> {
        log.debug("Received channel data for $message")
        message.contents.forEach { update ->
            if(markets.containsKey(update.key)) {
                val marketInfo = MarketInfo(
                    symbol = update.key,
                    status = MarketStatus.valueOf(update.value.status ?: markets[update.key]?.status.toString()),
                    indexPrice = update.value.indexPrice?.toFloatOrNull() ?: markets[update.key]?.indexPrice!!,
                    oraclePrice = update.value.oraclePrice?.toFloatOrNull() ?: markets[update.key]?.oraclePrice!!
                )
                markets[update.key] = marketInfo

            }
        }

        return markets.filterKeys { it in message.contents.keys }.values.toList()
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }
}