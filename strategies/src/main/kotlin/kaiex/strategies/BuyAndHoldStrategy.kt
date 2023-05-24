package kaiex.strategies

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.strategy.KaiexBaseStrategy
import kaiex.strategy.StrategyParams
import kaiex.ui.SeriesColor
import kaiex.ui.createChart

/**
 * Strategy that simply buys and HODLs
 */
class BuyAndHoldStrategy(private val parameters: Map<String, String>): KaiexBaseStrategy() {

    // extract params
    private val strategyParams = StrategyParams(parameters, setOf("symbol"))
    private val symbol = strategyParams.getString("symbol")
    private val size = strategyParams.getDouble("size")

    private val chart = createChart("Default") {
        candleSeries("Candles") {
            upColor = SeriesColor.GREEN.rgb
            downColor = SeriesColor.RED.rgb
        }
        valueSeries("Position") {
            color = SeriesColor.GREY.rgb
        }
    }

    override fun onCreate() {
        log.info("onCreate: $parameters")
        addSymbol(symbol)
        addChart(chart)
    }

    override fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {
        log.info("onMarketData: $snapshot")

        val candle = snapshot[symbol]?.lastCandle
        if (candle?.complete == true) {

            setPosition(symbol, size)

            chart.update(candle.startTimestamp.toEpochMilli()) {
                "Candles"(listOf(candle.open.toDouble(), candle.high.toDouble(), candle.low.toDouble(), candle.close.toDouble()))
                "Position"(getCurrentPosition(symbol).toDouble())
            }
        }
    }

    override fun onOrderUpdate(update: OrderUpdate) {
        log.info("onOrderUpdate: $update")
    }

    override fun onDestroy() {
        log.info("onStop()")
    }
}