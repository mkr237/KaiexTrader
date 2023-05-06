package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyConfig
import kaiex.ui.StrategyMarketDataUpdate

/**
 * Simple MACD Strategy
 */
class MACDStrategy: KaiexBaseStrategy() {

    companion object {
        val config = StrategyConfig(
            strategyId = "MACDStrategy:BTC-USD",
            strategyType = "kaiex.strategy.MACDStrategy",
            strategyDescription = "Simple MACD Strategy",
            symbols = listOf("BTC-USD"),
            parameters = mapOf(),
            chartConfig = listOf(
                ChartSeriesConfig("price", "candle", 0, "#00FF00"),
                ChartSeriesConfig("histogram", "histogram", 1, "#26a69a"),
                ChartSeriesConfig("macd", "line", 1, "#2196F3"),
                ChartSeriesConfig("signal", "line", 1, "#FC6C02")
            )
        )
    }

    private var symbol:String? = null
    private val macd = MACD(12, 26, 9)
    private val positionSize = 0.02f

    override fun onStrategyCreate() {
        log.info("onStrategyCreate()")
        symbol = config.symbols[0]

        addIndicator("MACD", symbol!!, macd)
    }

    override fun onStrategyMarketData(snapshot: Map<String, MarketDataSnapshot>) {

        val candle = snapshot[symbol]?.lastCandle!!
        if(candle.complete) {

            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()

            if (!candle.historical && macdLine > signalLine) {
                log.info("Detected LONG signal - MACD: $macdLine > Signal: $signalLine")
                setPosition(symbol!!, positionSize)
            } else if (!candle.historical && macdLine < signalLine) {
                log.info("Detected SHORT signal - MACD: $macdLine < Signal: $signalLine\"")
                setPosition(symbol!!, -positionSize)
            }

            val updates = listOf(
                SeriesUpdate.NumericUpdate(id = "macd", value = macdLine),
                SeriesUpdate.NumericUpdate(id = "signal", value = signalLine),
                SeriesUpdate.NumericUpdate(id = "histogram", value = histogram)
            )

            uiServer.send(StrategyMarketDataUpdate(config.strategyId, candle.startTimestamp, updates))
        }
    }

    override fun onStrategyOrderUpdate(update: OrderUpdate) {
        log.info("Order Update: $update")
    }

    override fun onStrategyDestroy() {
        log.info("onStop()")
    }
}