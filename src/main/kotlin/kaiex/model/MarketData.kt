package kaiex.model

import kotlinx.coroutines.flow.Flow
import java.time.Instant

enum class Side {
    BUY,
    SELL
}

data class Candle (                 // TODO do we want these to be var rather than val?
    var startTimestamp: Instant,
    var open: Float,
    var high: Float,
    var low: Float,
    var close: Float,
    var volume: Float      // TODO Int?
)

data class Trade (
    val symbol:String,
    val side:Side,
    val size:Float,
    val price:Float,
    val createdAt:Instant,
    val receivedAt:Instant,
    val liquidation:Boolean
)

data class OrderBookEntry (
    val size:Float,
    val price:Float
)

data class OrderBook (
    val symbol:String,
    val bids:List<OrderBookEntry>,
    val asks:List<OrderBookEntry>,
    val receivedTime:Instant
)

interface MarketDataService {
    suspend fun subscribeTrades(symbol: String): Flow<Trade>
    suspend fun unsubscribeTrades(symbol: String)
    suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook>
    suspend fun unsubscribeOrderBook(symbol: String)
}
