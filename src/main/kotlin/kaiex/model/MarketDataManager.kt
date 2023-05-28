package kaiex.model

import kaiex.exchange.ExchangeService
import kaiex.indicator.Indicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

class MarketDataManager: KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService: ExchangeService by inject()

    private val snapshots: MutableMap<String, MarketDataSnapshot> = ConcurrentHashMap()
    private val listener = MutableSharedFlow<Map<String, MarketDataSnapshot>>()

    fun addSymbol(symbol: String) {
        snapshots[symbol] = MarketDataSnapshot()
    }

    fun addIndicator(name: String, symbol: String, indicator: Indicator) {
        log.info("Adding indicator $name for $symbol")
        val snapshot = snapshots.computeIfAbsent(symbol) { MarketDataSnapshot() }
        snapshot.indicators.putIfAbsent(name, indicator)
    }

    suspend fun startAndSubscribe(scope: CoroutineScope): SharedFlow<Map<String, MarketDataSnapshot>> {
        scope.launch { trackMarketInfo() }
        snapshots.keys.forEach { symbol ->
            scope.launch { trackTrades(symbol) }
        }
        return listener.asSharedFlow()
    }

    private suspend fun trackMarketInfo() {
        log.info("Tracking market info")
        exchangeService.subscribeMarketInfo().collect { marketInfo: MarketInfo ->
            // only store/send market info for the symbols we're interested in
            if(snapshots.keys.contains(marketInfo.symbol)) {
                snapshots[marketInfo.symbol]?.marketInfo = marketInfo
                listener.emit(snapshots)
            }
        }
    }

    // TODO validate symbol against received market info
    private suspend fun trackTrades(symbol: String) {
        log.info("Tracking trades for $symbol")
        exchangeService.subscribeTrades((symbol))
            .onCompletion {
                listener.emit(ConcurrentHashMap())
            }
            .onEach { trade:Trade ->
                snapshots[symbol]?.lastTrade = trade
            }
            .toCandles(ChronoUnit.MINUTES).collect { candle: Candle ->
                snapshots[symbol]?.lastCandle = candle

                // if the candle is complete, update indicators
                if(candle.complete) {
                    snapshots[symbol]?.indicators?.values?.forEach { indicator ->
                        indicator.update(candle.close.toDouble())
                    }
                }
                listener.emit(snapshots)
            }
    }

    private suspend fun trackOrderBook(symbol: String) {
        log.info("Tracking order book for $symbol")
        exchangeService.subscribeOrderBook(symbol).collect { orderBook:OrderBook ->
            snapshots[symbol]?.lastOrderBook = orderBook
            listener.emit(snapshots)
        }
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