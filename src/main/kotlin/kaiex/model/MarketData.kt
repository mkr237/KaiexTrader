package kaiex.model

import kaiex.indicator.Indicator
import kotlinx.coroutines.flow.Flow
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
    val indexPrice: Float,
    val oraclePrice: Float,
    val createdAt: Instant
)

data class Candle (
    val symbol:String,
    val startTimestamp: Long,
    val lastUpdate: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val numTrades: Int,
    val volume: Float,
    val historical: Boolean,
    val complete: Boolean
)

data class Trade (
    val symbol:String,
    val side:OrderSide,
    val size:Float,
    val price:Float,
    val createdAt:Instant,
    val receivedAt:Instant,
    val liquidation:Boolean,
    val historical: Boolean
)

data class OrderBookEntry (
    val size:Float,
    val price:Float
)

data class OrderBook (
    val sequenceNumber:Int,
    val symbol:String,
    val bids:List<OrderBookEntry>,
    val asks:List<OrderBookEntry>,
    val receivedAt:Instant
)

data class MarketDataSnapshot (
    var marketInfo: MarketInfo? = null,
    var lastCandle: Candle? = null,
    var lastTrade: Trade? = null,
    var lastOrderBook: OrderBook? = null,
    var indicators: MutableMap<String, Indicator> = mutableMapOf()
)

interface MarketDataService {
    suspend fun subscribeMarketInfo(): Flow<MarketInfo>
    suspend fun unsubscribeMarketInfo()
    suspend fun subscribeTrades(symbol: String): Flow<Trade>
    suspend fun unsubscribeTrades(symbol: String)
    suspend fun subscribeCandles(symbol: String): Flow<Candle>
    suspend fun unsubscribeCandles(symbol: String)
    suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook>
    suspend fun unsubscribeOrderBook(symbol: String)
}
