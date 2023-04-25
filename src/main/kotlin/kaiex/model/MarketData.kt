package kaiex.model

import kaiex.indicator.Indicator
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
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
    val oraclePrice: Float
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

class MarketDataSnapshot {

    private val maxHistoryLength = 10
    private val marketInfo: MutableMap<String, MarketInfo> = mutableMapOf()
    private val candles: MutableMap<String, MutableList<Candle>> = mutableMapOf()
    private val trades: MutableMap<String, MutableList<Trade>> = mutableMapOf()
    private val orderBooks: MutableMap<String, MutableList<OrderBook>> = mutableMapOf()
    private val indicators: MutableMap<String, MutableMap<String, Indicator>> = mutableMapOf()

    fun updateMarketInfo(info: MarketInfo) {
        marketInfo[info.symbol] = info
    }

    fun updateCandle(candle: Candle) {
        val candlesForSymbol = getCandles(candle.symbol)
        candlesForSymbol.add(candle)
        if (candlesForSymbol.size > maxHistoryLength) {
            candlesForSymbol.removeAt(0)
        }
    }

    fun updateTrade(trade: Trade) {
        val tradesForSymbol = getTrades(trade.symbol)
        tradesForSymbol.add(trade)
        if (tradesForSymbol.size > maxHistoryLength) {
            tradesForSymbol.removeAt(0)
        }
    }

    fun updateOrderBook(book: OrderBook) {
        val orderBooksForSymbol = getOrderBooks(book.symbol)
        orderBooksForSymbol.add(book)
        if (orderBooksForSymbol.size > maxHistoryLength) {
            orderBooksForSymbol.removeAt(0)
        }
    }

    fun updateIndicator(indicator: Indicator) {

    }

    fun getMarketInfo(symbol: String): MarketInfo? {
        return marketInfo[symbol]
    }

    fun getCandles(symbol: String): MutableList<Candle> {
        val candlesForSymbol = candles[symbol]
        if (candlesForSymbol == null) {
            candles[symbol] = mutableListOf()
        }
        return candles[symbol]!!
    }

    fun getTrades(symbol: String): MutableList<Trade> {
        val tradesForSymbol = trades[symbol]
        if (tradesForSymbol == null) {
            trades[symbol] = mutableListOf()
        }
        return trades[symbol]!!
    }

    fun getOrderBooks(symbol: String): MutableList<OrderBook> {
        val orderBooksForSymbol = orderBooks[symbol]
        if (orderBooksForSymbol == null) {
            orderBooks[symbol] = mutableListOf()
        }
        return orderBooks[symbol]!!
    }
}


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
