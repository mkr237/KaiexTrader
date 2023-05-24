package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.exchange.ExchangeException
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
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
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
                        is Subscribed -> onSubscribed(dydxMarketsUpdate).values.forEach{ market -> emit(market) }
                        is ChannelData -> onChannelData(dydxMarketsUpdate).values.forEach{ market -> emit(market) }
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

    private fun onSubscribed(message: Subscribed):Map<String, kaiex.model.MarketInfo> {
        log.debug("Subscribed to $message")
        markets = mutableMapOf()
        message.contents.markets.forEach { (symbol, data) ->
            markets[symbol] = MarketInfo(
                symbol = symbol,
                status = parseMarketStatus(data.status!!),
                indexPrice = data.indexPrice?.toBigDecimal() ?: BigDecimal.ZERO,
                oraclePrice = data.oraclePrice?.toBigDecimal() ?: BigDecimal.ZERO,
                baseAsset = data.baseAsset ?: "",
                quoteAsset = data.quoteAsset ?: "",
                stepSize = data.stepSize?.toBigDecimal() ?: BigDecimal.ZERO,
                tickSize = data.tickSize?.toBigDecimal() ?: BigDecimal.ZERO,
                priceChange24H = data.priceChange24H?.toBigDecimal() ?: BigDecimal.ZERO,
                nextFundingRate = data.nextFundingRate?.toBigDecimal() ?: BigDecimal.ZERO,
                nextFundingAt = parseNextFundingAt(data.nextFundingAt!!),
                minOrderSize = data.minOrderSize?.toBigDecimal() ?: BigDecimal.ZERO,
                type = parseMarketType(data.type!!),
                initialMarginFraction = data.initialMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
                maintenanceMarginFraction = data.maintenanceMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
                transferMarginFraction = data.transferMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
                volume24H = data.volume24H?.toBigDecimal() ?: BigDecimal.ZERO,
                trades24H = data.trades24H?.toInt() ?: 0,
                openInterest = data.openInterest?.toBigDecimal() ?: BigDecimal.ZERO,
                incrementalInitialMarginFraction = data.incrementalInitialMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
                incrementalPositionSize = data.incrementalPositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
                maxPositionSize = data.maxPositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
                baselinePositionSize = data.baselinePositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
                assetResolution = data.assetResolution?.toBigInteger() ?: BigInteger.ZERO,
                syntheticAssetId = data.syntheticAssetId ?: ""
            )
        }
        return markets
    }

    private fun onChannelData(message: ChannelData):Map<String, kaiex.model.MarketInfo> {
        log.debug("Received channel data for $message")
        message.contents.forEach { (symbol, data) ->
            if(markets.containsKey(symbol)) {
                val lastMarket = markets[symbol]
                markets[symbol] = MarketInfo(
                    symbol = symbol,
                    status = if(data.status != null) { parseMarketStatus(data.status) } else { lastMarket!!.status },
                    indexPrice = data.indexPrice?.toBigDecimal() ?: lastMarket!!.indexPrice,
                    oraclePrice = data.oraclePrice?.toBigDecimal() ?: lastMarket!!.oraclePrice,
                    baseAsset = data.baseAsset ?: lastMarket!!.baseAsset,
                    quoteAsset = data.quoteAsset ?: lastMarket!!.quoteAsset,
                    stepSize = data.stepSize?.toBigDecimal() ?: lastMarket!!.stepSize,
                    tickSize = data.tickSize?.toBigDecimal() ?: lastMarket!!.tickSize,
                    priceChange24H = data.priceChange24H?.toBigDecimal() ?: lastMarket!!.priceChange24H,
                    nextFundingRate = data.nextFundingRate?.toBigDecimal() ?: lastMarket!!.nextFundingRate,
                    nextFundingAt = if(data.nextFundingAt != null) { parseNextFundingAt(data.nextFundingAt) } else { lastMarket!!.nextFundingAt },
                    minOrderSize = data.minOrderSize?.toBigDecimal() ?: lastMarket!!.minOrderSize,
                    type = if(data.type != null) { parseMarketType(data.type) } else { lastMarket!!.type },
                    initialMarginFraction = data.initialMarginFraction?.toBigDecimal() ?: lastMarket!!.initialMarginFraction,
                    maintenanceMarginFraction = data.maintenanceMarginFraction?.toBigDecimal() ?: lastMarket!!.maintenanceMarginFraction,
                    transferMarginFraction = data.transferMarginFraction?.toBigDecimal() ?: lastMarket!!.transferMarginFraction,
                    volume24H = data.volume24H?.toBigDecimal() ?: lastMarket!!.volume24H,
                    trades24H = data.trades24H?.toInt() ?: lastMarket!!.trades24H,
                    openInterest = data.openInterest?.toBigDecimal() ?: lastMarket!!.openInterest,
                    incrementalInitialMarginFraction = data.incrementalInitialMarginFraction?.toBigDecimal() ?: lastMarket!!.incrementalInitialMarginFraction,
                    incrementalPositionSize = data.incrementalPositionSize?.toBigDecimal() ?: lastMarket!!.incrementalPositionSize,
                    maxPositionSize = data.maxPositionSize?.toBigDecimal() ?: lastMarket!!.maxPositionSize,
                    baselinePositionSize = data.baselinePositionSize?.toBigDecimal() ?: lastMarket!!.baselinePositionSize,
                    assetResolution = data.assetResolution?.toBigInteger() ?: lastMarket!!.assetResolution,
                    syntheticAssetId = data.syntheticAssetId ?: lastMarket!!.syntheticAssetId
                )
            }
        }

        return markets
    }

    private fun parseMarketStatus(statusStr: String): MarketStatus {
        return try {
            MarketStatus.valueOf(statusStr)
        } catch (e: IllegalArgumentException) {
            MarketStatus.OFFLINE
        }
    }

    private fun parseMarketType(typeStr: String): MarketType {
        return try {
            MarketType.valueOf(typeStr)
        } catch (e: IllegalArgumentException) {
            throw ExchangeException("Cannot market info update: Invalid field type: $typeStr")
        }
    }

    private fun parseNextFundingAt(atStr: String): Instant {
        return try {
            Instant.parse(atStr)
        } catch (e: IllegalArgumentException) {
            throw throw ExchangeException("Cannot market info update: Invalid field nextFundingAt: $atStr")
        }
    }

    private fun convertMarketInfo(symbol: String, data: MarketInfo) = MarketInfo(
        symbol = symbol,
        status = parseMarketStatus(data.status!!),
        indexPrice = data.indexPrice?.toBigDecimal() ?: BigDecimal.ZERO,
        oraclePrice = data.oraclePrice?.toBigDecimal() ?: BigDecimal.ZERO,
        baseAsset = data.baseAsset ?: "",
        quoteAsset = data.quoteAsset ?: "",
        stepSize = data.stepSize?.toBigDecimal() ?: BigDecimal.ZERO,
        tickSize = data.tickSize?.toBigDecimal() ?: BigDecimal.ZERO,
        priceChange24H = data.priceChange24H?.toBigDecimal() ?: BigDecimal.ZERO,
        nextFundingRate = data.nextFundingRate?.toBigDecimal() ?: BigDecimal.ZERO,
        nextFundingAt = Instant.parse(data.nextFundingAt) ?: throw ExchangeException("Cannot market info update: Invalid field nextFundingAt: ${data.nextFundingAt}"),
        minOrderSize = data.minOrderSize?.toBigDecimal() ?: BigDecimal.ZERO,
        type = parseMarketType(data.type!!),
        initialMarginFraction = data.initialMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
        maintenanceMarginFraction = data.maintenanceMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
        transferMarginFraction = data.transferMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
        volume24H = data.volume24H?.toBigDecimal() ?: BigDecimal.ZERO,
        trades24H = data.trades24H?.toInt() ?: 0,
        openInterest = data.openInterest?.toBigDecimal() ?: BigDecimal.ZERO,
        incrementalInitialMarginFraction = data.incrementalInitialMarginFraction?.toBigDecimal() ?: BigDecimal.ZERO,
        incrementalPositionSize = data.incrementalPositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
        maxPositionSize = data.maxPositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
        baselinePositionSize = data.baselinePositionSize?.toBigDecimal() ?: BigDecimal.ZERO,
        assetResolution = data.assetResolution?.toBigInteger() ?: BigInteger.ZERO,
        syntheticAssetId = data.syntheticAssetId ?: ""
    )

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }
}