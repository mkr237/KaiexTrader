package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.StrategyConfig
import kotlinx.coroutines.delay

/**
 * Strategy that simply buys and HODLs
 */
class BuyAndHoldStrategy: KaiexBaseStrategy() {

    companion object {
        val config = StrategyConfig(
            strategyId = "BuyAndHoldStrategy:BTC-USD",
            strategyType = "kaiex.strategy.BuyAndHoldStrategy",
            strategyDescription = "Strategy that simply buys BTC-USD and HODLs",
            symbols = listOf("BTC-USD"),
            parameters = mapOf("foo" to "bar"),
            chartConfig = listOf(
                ChartSeriesConfig("price", "candle", 0, "#00FF00"),
            )
        )
    }

    private var symbol:String? = null

    override fun onStrategyCreate() {
        log.info("onStrategyCreate()")
        symbol = config.symbols[0]

        // buy and HODL
        //delay(5000)
        //buyAtMarket(symbol!!, 0.01f)
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

    override fun onStrategyDestroy() {
        log.info("onStop()")
    }
}