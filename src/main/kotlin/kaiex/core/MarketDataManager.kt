package kaiex.core

import com.kaiex.model.Trade
import com.kaiex.services.dydx.DYDXExchangeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set

class MarketDataManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val dydxExchangeService : DYDXExchangeService by inject()

    // TODO maintain a refCount to allow unsubscribes
    private val tradeBroadcasters:MutableMap<String, EventBroadcaster<Trade>> = mutableMapOf()

    suspend fun subscribeTrades(symbol: String): EventBroadcaster<Trade> {
        if(!tradeBroadcasters.containsKey(symbol)) {
            log.info("Subscribing to trades for $symbol")
            tradeBroadcasters[symbol] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                dydxExchangeService.subscribeTrades((symbol)).collect { trade ->
                    tradeBroadcasters[symbol]?.sendEvent(trade) }
            }
        } else {
            log.info("Subscription exists for $symbol")
        }

        return tradeBroadcasters[symbol] ?: throw RuntimeException("Unknown Symbol: $symbol")
    }

    fun unsubscribeTrades(symbol: String) {

    }

//    fun subscribeOrderBook(symbol: String) {
//
//        log.info("Subscribing to orderbooks")
//        dydxExchangeService.subscribeOrderBook("BTC-USD").onEach { ob ->
//            log.info(ob.toString())
//        }.launchIn(this)
//    }

    fun unsubscribeOrderBook(symbol: String) {

    }
}