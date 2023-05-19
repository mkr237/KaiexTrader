package kaiex.strategy

import kaiex.exchange.ExchangeService
import kaiex.indicator.Indicator
import kaiex.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.util.concurrent.ConcurrentHashMap

class MarketDataTracker(val onUpdate: suspend (Map<String, MarketDataSnapshot>) -> Unit) : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService: ExchangeService by inject()

    private val snapshots: MutableMap<String, MarketDataSnapshot> = ConcurrentHashMap()

    fun trackMarketInfo(): Job = CoroutineScope(Dispatchers.Default).launch {
        log.info("Tracking market info")
        exchangeService.subscribeMarketInfo().collect { marketInfo: MarketInfo ->
            // only store/send market info for the symbols we're interested in
            if(snapshots.keys.contains(marketInfo.symbol)) {
                val snapshot = snapshots.computeIfAbsent(marketInfo.symbol) { MarketDataSnapshot() }
                snapshot.marketInfo = marketInfo
                onUpdate(snapshots)
            }
        }
    }

    // TODO validate symbol against received market info
    fun trackTrades(symbol: String): Job = CoroutineScope(Dispatchers.Default).launch {
        log.info("Tracking trades for $symbol")
        snapshots.computeIfAbsent(symbol) { MarketDataSnapshot() }
        exchangeService.subscribeTrades((symbol))
            .onEach { trade:Trade ->
                snapshots[symbol]?.lastTrade = trade
            }
            .toCandles().collect { candle: Candle ->
                snapshots[symbol]?.lastCandle = candle

                // if the candle is complete, update indicators
                if(candle.complete) {
                    snapshots[symbol]?.indicators?.values?.forEach { indicator ->
                        indicator.update(candle.close.toDouble())
                    }
                }
                onUpdate(snapshots)
            }
    }

    fun trackOrderBook(symbol: String): Job = CoroutineScope(Dispatchers.Default).launch {
        log.info("Tracking order book for $symbol")
        exchangeService.subscribeOrderBook(symbol).collect { orderBook:OrderBook ->
            val snapshot = snapshots.computeIfAbsent(orderBook.symbol) { MarketDataSnapshot() }
            snapshot.lastOrderBook = orderBook
            onUpdate(snapshots)
        }
    }

    fun addIndicator(name: String, symbol: String, indicator: Indicator) {
        log.info("Adding indicator $name for $symbol")
        val snapshot = snapshots.computeIfAbsent(symbol) { MarketDataSnapshot() }
        snapshot.indicators.putIfAbsent(name, indicator)
    }
}

//fun main() {
//    val tracker = marketDataTracker {
//        trades {
//            symbol = "BTC-USD"
//            period = Duration.ofMinutes(1)
//        }
//        orderbook {
//            symbol = "BTC-USD"
//            maxDepth = 10
//        }
//        indicator(MACD) {
//            fastPeriod = 26
//            slowPerdio = 20
//            signal = 9
//        }
//        onUpdate {
//            // to be called when any market data changes
//        }
//    }
//}