package kaiex.core

import kaiex.exchange.ExchangeService
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kaiex.ui.SeriesUpdate
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import kotlin.collections.set

class MarketDataManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService: ExchangeService by inject()

    // TODO maintain a refCount to allow unsubscribes
    private val marketInfoBroadcasters:MutableMap<String, EventBroadcaster<MarketInfo>> = mutableMapOf()
    private val tradeBroadcasters:MutableMap<String, EventBroadcaster<Trade>> = mutableMapOf()
    private val candleBroadcasters:MutableMap<String, EventBroadcaster<Candle>> = mutableMapOf()
    private val orderBookBroadcasters:MutableMap<String, EventBroadcaster<OrderBook>> = mutableMapOf()

    init {
        log.info("Starting")
        CoroutineScope(Dispatchers.Default).launch {// TODO launch with a launch!
            launch {
                exchangeService.subscribeMarketInfo().collect { marketInfo: MarketInfo ->
                    if(marketInfoBroadcasters.containsKey(marketInfo.symbol)) {
                        marketInfoBroadcasters[marketInfo.symbol]?.sendEvent(marketInfo)
                    }
                }
            }
        }
    }

    fun subscribeMarketInfo(symbol: String): EventBroadcaster<MarketInfo> {
        if(!marketInfoBroadcasters.containsKey(symbol)) {
            marketInfoBroadcasters[symbol] = EventBroadcaster()
        } else {
            log.info("MarketInfo subscription exists for $symbol")
        }

        return marketInfoBroadcasters[symbol] ?: throw RuntimeException("Unknown Symbol: $symbol")
    }

    fun unsubscribeMarketInfo() {
        // TODO Not Implemented
    }

    fun subscribeTrades(symbol: String): EventBroadcaster<Trade> {
        if(!tradeBroadcasters.containsKey(symbol)) {
            log.info("Subscribing to trades for $symbol")
            tradeBroadcasters[symbol] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                exchangeService.subscribeTrades((symbol)).collect { trade:Trade ->
                    tradeBroadcasters[symbol]?.sendEvent(trade)
                }
            }
        } else {
            log.info("Trade subscription exists for $symbol")
        }

        return tradeBroadcasters[symbol] ?: throw RuntimeException("Unknown Symbol: $symbol")
    }

    fun unsubscribeTrades(symbol: String) {
        // TODO Not Implemented
    }

    fun subscribeCandles(symbol: String): EventBroadcaster<Candle> {
        if(!candleBroadcasters.containsKey(symbol)) {
            log.info("Subscribing to candles for $symbol")
            candleBroadcasters[symbol] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                subscribeTrades(symbol).listenForEvents().toCandles().collect { candle ->
                    candleBroadcasters[symbol]?.sendEvent(candle)
                }
            }

        } else {
            log.info("Candle subscription exists for $symbol")
        }

        return candleBroadcasters[symbol] ?: throw RuntimeException("Unknown Symbol: $symbol")
    }

    fun subscribeOrderBook(symbol: String): EventBroadcaster<OrderBook> {
        if(!orderBookBroadcasters.containsKey(symbol)) {
            log.info("Subscribing to order book for $symbol")
            orderBookBroadcasters[symbol] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                exchangeService.subscribeOrderBook((symbol)).collect { ob: OrderBook ->
                    orderBookBroadcasters[symbol]?.sendEvent((ob))
                }
            }
        } else {
            log.info("OrderBook subscription exists for $symbol")
        }

        return orderBookBroadcasters[symbol] ?: throw RuntimeException("Unknown Symbol: $symbol")
    }

    fun unsubscribeOrderBook(symbol: String) {
        // TODO Not Implemented
    }
}