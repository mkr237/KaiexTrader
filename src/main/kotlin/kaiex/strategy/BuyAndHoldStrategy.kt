package kaiex.strategy

import kaiex.model.OrderUpdate
import kotlinx.coroutines.delay

/**
 * Strategy that simply buys and HODLs
 */
class BuyAndHoldStrategy: KaiexBaseStrategy() {

    override suspend fun onStrategyCreate() {
        log.info("onStrategyCreate()")
        delay(5000)     // TODO Why?
        buyAtMarket(symbol = config.symbols[0], size = 0.01f)
    }

    override fun onStrategyMarketData() {
        log.info("onStrategyMarketData()")
    }

    override fun onStrategyOrderUpdate(update: OrderUpdate) {
        log.info("onStrategyOrderUpdate()")
    }

    override suspend fun onStrategyDestroy() {
        log.info("onStop()")
    }
}