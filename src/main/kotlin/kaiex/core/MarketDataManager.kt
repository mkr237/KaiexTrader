package kaiex.core

import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.set

class MarketDataManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val dydxExchangeService : DYDXExchangeService by inject()

    // TODO maintain a refCount to allow unsubscribes
    private val marketInfoBroadcasters:MutableMap<String, EventBroadcaster<MarketInfo>> = mutableMapOf()
    private val tradeBroadcasters:MutableMap<String, EventBroadcaster<Trade>> = mutableMapOf()
    private val orderBookBroadcasters:MutableMap<String, EventBroadcaster<OrderBook>> = mutableMapOf()

    private val tradeSubscriptions:MutableMap<String, Flow<Trade>> = mutableMapOf()

    init {
        log.info("Starting")
        CoroutineScope(Dispatchers.Default).launch {// TODO launch with a launch!
            launch {
                dydxExchangeService.subscribeMarketInfo().collect { marketInfo: MarketInfo ->
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
                dydxExchangeService.subscribeTrades((symbol)).collect { trade:Trade ->
                    tradeBroadcasters[symbol]?.sendEvent(trade)
                }
                while(true) {
                    delay(5000)
                    println("HERE")
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

    fun subscribeOrderBook(symbol: String): EventBroadcaster<OrderBook> {
        if(!orderBookBroadcasters.containsKey(symbol)) {
            orderBookBroadcasters[symbol] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                dydxExchangeService.subscribeOrderBook((symbol)).collect { ob: OrderBook ->
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