package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate

/**
 * Strategy that simply buys and HODLs
 */
class BuyAndHoldStrategy: KaiexBaseStrategy() {

    private var symbol:String? = null

    override fun onCreate() {
        log.info("onStrategyCreate()")

        // buy and HODL
        //delay(5000)
        //buyAtMarket(symbol!!, 0.01f)
    }

    override fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {
        log.info("*** Received market data snapshot ***")
        log.info("Market Data Info: ${snapshot[symbol]?.marketInfo ?: "-"}")
        log.info("Last Candle: ${snapshot[symbol]?.lastCandle ?: "-"}")
        log.info("Last Order Book: ${snapshot[symbol]?.lastOrderBook ?: "-"}")
    }

    override fun onOrderUpdate(update: OrderUpdate) {
        log.info("*** Received order update ***")
        log.info("Order Update: $update")
    }

    override fun onDestroy() {
        log.info("onStop()")
    }
}