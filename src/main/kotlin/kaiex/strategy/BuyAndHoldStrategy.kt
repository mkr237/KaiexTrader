package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kotlinx.coroutines.delay

/**
 * Strategy that simply buys and HODLs
 */
class BuyAndHoldStrategy: KaiexBaseStrategy() {

    private var symbol:String? = null

    override suspend fun onStrategyCreate() {
        log.info("onStrategyCreate()")
        symbol = config.symbols[0]

        // buy and HODL
        delay(5000)
        buyAtMarket(symbol!!, 0.01f)
    }

    override fun onStrategyMarketData(snapshot: MarketDataSnapshot) {
        log.info("*** Received market data snapshot ***")
        log.info("Market Data Info: ${snapshot.getMarketInfo(symbol!!) ?: "-"}")
        log.info("Candles: ${snapshot.getCandles(symbol!!).lastOrNull() ?: "-"}")
        log.info("Order Book: ${snapshot.getOrderBooks(symbol!!).lastOrNull() ?: "-"}")
    }

    override fun onStrategyOrderUpdate(update: OrderUpdate) {
        log.info("*** Received order update ***")
        log.info("Order Update: $update")
    }

    override suspend fun onStrategyDestroy() {
        log.info("onStop()")
    }
}