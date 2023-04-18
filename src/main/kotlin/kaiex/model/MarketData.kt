package kaiex.model

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Candle (
    val startTimestamp: Long,
    val lastUpdate: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val numTrades: Int,
    val volume: Float,
    val historical: Boolean
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

interface MarketDataService {
    suspend fun subscribeTrades(symbol: String): Flow<Trade>
    suspend fun unsubscribeTrades(symbol: String)
    suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook>
    suspend fun unsubscribeOrderBook(symbol: String)
}
