package kaiex.model

import kaiex.indicator.Indicator
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

enum class MarketStatus {
    ONLINE,
    OFFLINE,
    POST_ONLY,
    CANCEL_ONLY,
    CLOSE_ONLY
}

data class MarketInfo (
    val symbol:String,
    val status: MarketStatus,
    val indexPrice: BigDecimal,
    val oraclePrice: BigDecimal,
    val createdAt: Instant
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
