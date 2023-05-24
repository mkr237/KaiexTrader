package kaiex.model

import kaiex.indicator.Indicator
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

enum class MarketStatus {
    ONLINE,
    OFFLINE,
    POST_ONLY,
    CANCEL_ONLY,
    CLOSE_ONLY
}

enum class MarketType {
    PERPETUAL
}

data class MarketInfo (
    val symbol:String,
    val status: MarketStatus,
    val baseAsset: String,
    val quoteAsset: String,
    val stepSize: BigDecimal,
    val tickSize: BigDecimal,
    val indexPrice: BigDecimal,
    val oraclePrice: BigDecimal,
    val priceChange24H: BigDecimal,
    val nextFundingRate: BigDecimal,
    val nextFundingAt: Instant,
    val minOrderSize: BigDecimal,
    val type: MarketType,
    val initialMarginFraction: BigDecimal,
    val maintenanceMarginFraction: BigDecimal,
    val transferMarginFraction: BigDecimal,
    val volume24H: BigDecimal,
    val trades24H: Int,
    val openInterest: BigDecimal,
    val incrementalInitialMarginFraction: BigDecimal,
    val incrementalPositionSize: BigDecimal,
    val maxPositionSize: BigDecimal,
    val baselinePositionSize: BigDecimal,
    val assetResolution: BigInteger,
    val syntheticAssetId: String
)

data class Candle (
    val symbol:String,
    val startTimestamp: Instant,
    val lastUpdate: Instant,
    val open: BigDecimal,
    val high: BigDecimal,
    val low: BigDecimal,
    val close: BigDecimal,
    val numTrades: Int,
    val volume: BigDecimal,
    val historical: Boolean,
    val complete: Boolean
)

data class Trade (
    val symbol:String,
    val side:OrderSide,
    val size:BigDecimal,
    val price:BigDecimal,
    val createdAt:Instant,
    val receivedAt:Instant,
    val liquidation:Boolean,
    val historical: Boolean
)

data class OrderBookEntry (
    val size:BigDecimal,
    val price:BigDecimal
)

data class OrderBook (
    val sequenceNumber:Int,
    val symbol:String,
    val bids:List<OrderBookEntry>,
    val asks:List<OrderBookEntry>,
    val receivedAt:Instant
)

data class MarketDataSnapshot (
    var timestamp: Instant? = null,
    var marketInfo: MarketInfo? = null,
    var lastCandle: Candle? = null,
    var lastTrade: Trade? = null,
    var lastOrderBook: OrderBook? = null,
    var indicators: MutableMap<String, Indicator> = mutableMapOf()
)

interface MarketDataService {
    suspend fun subscribeMarketInfo(): Flow<MarketInfo>
    suspend fun subscribeTrades(symbol: String): Flow<Trade>
    suspend fun subscribeCandles(symbol: String): Flow<Candle>
    suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook>
}
