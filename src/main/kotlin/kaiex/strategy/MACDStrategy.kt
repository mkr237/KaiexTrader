package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.SeriesColor
import kaiex.ui.createChart

/**
 * Simple MACD Strategy
 */
class MACDStrategy(private val parameters: Map<String, String>): KaiexBaseStrategy() {

    // extract params
    private val strategyParams = StrategyParams(parameters, setOf("symbol"))
    private val symbol = strategyParams.getString("symbol")
    private val fastPeriod = strategyParams.getInt("fast")
    private val slowPeriod = strategyParams.getInt("slow")
    private val signalPeriod = strategyParams.getInt("signal")

    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)
    private val positionSize = 0.02f

    private val chart = createChart("Default") {
        candleSeries("Candles") {
            upColor = SeriesColor.GREEN.rgb
            downColor = SeriesColor.RED.rgb
        }
        valueSeries("MACD") {
            color = SeriesColor.BLUE.rgb
        }
        valueSeries("Signal") {
            color = SeriesColor.ORANGE.rgb
        }
        valueSeries("Position") {
            color = SeriesColor.GREY.rgb
        }
    }

    override fun onStrategyCreate() {
        log.info("onStrategyCreate: $parameters")
        reportManager.addChart(chart)
        addTrades(symbol)
        addIndicator("MACD", symbol, macd)
    }

    override fun onStrategyMarketData(snapshot: Map<String, MarketDataSnapshot>) {

        log.info("onStrategyMarketData: $snapshot")

        val candle = snapshot[symbol]?.lastCandle
        if (candle?.complete == true) {

            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()

            if (!candle.historical && macdLine > signalLine) {
                log.info("Detected LONG signal - MACD: $macdLine > Signal: $signalLine")
                setPosition(symbol, positionSize)
            } else if (!candle.historical && macdLine < signalLine) {
                log.info("Detected SHORT signal - MACD: $macdLine < Signal: $signalLine\"")
                setPosition(symbol, -positionSize)
            }

            chart.update(candle.startTimestamp) {
                "Candles"(listOf(candle.open.toDouble(), candle.high.toDouble(), candle.low.toDouble(), candle.close.toDouble()))
                "MACD"(macdLine)
                "Signal"(signalLine)
                "Position"(getCurrentPosition(symbol).toDouble())
            }
        }
    }

    override fun onStrategyOrderUpdate(update: OrderUpdate) {
        log.info("onStrategyOrderUpdate: $update")
    }

    override fun onStrategyDestroy() {
        log.info("onStrategyDestroy()")
    }
}