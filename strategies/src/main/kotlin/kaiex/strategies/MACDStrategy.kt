package kaiex.strategies

import kaiex.indicator.MACD
import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.strategy.KaiexBaseStrategy
import kaiex.strategy.StrategyParams
import kaiex.ui.LineShape
import kaiex.ui.SeriesColor
import kaiex.ui.createChart
import java.time.Instant

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
    private val positionSize = 0.02

    private val chart = createChart("Default") {
        plot("Trade Data") {
            height = 0.6
            candleSeries("Candles") {
                upColor = SeriesColor.GREEN.rgb
                downColor = SeriesColor.RED.rgb
            }
        }
        plot("MACD Indicator") {
            height = 0.2
            lineSeries("MACD") {
                color = SeriesColor.BLUE.rgb
            }
            lineSeries("Signal") {
                color = SeriesColor.ORANGE.rgb
            }
        }
        plot("Postion") {
            height = 0.2
            lineSeries("Position") {
                color = SeriesColor.GREY.rgb
                shape = LineShape.hv
            }
        }
    }

    override fun onCreate() {
        log.info("onCreate: $parameters")
        addSymbol(symbol)
        addIndicator("MACD", symbol, macd)
        addChart(chart)
    }

    override fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {

        log.info("onMarketData: $snapshot")

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

            // update chart
            chart.update(candle.startTimestamp.toEpochMilli()) {
                set("Candles", listOf(candle.open.toDouble(), candle.high.toDouble(), candle.low.toDouble(), candle.close.toDouble()))
                set("MACD", macdLine)
                set("Signal", signalLine)
                set("Position", getCurrentPosition(symbol).toDouble())
            }
        }
    }

    override fun onOrderUpdate(update: OrderUpdate) {
        log.info("onOrderUpdate: $update")
    }

    override fun onDestroy() {
        log.info("onDestroy()")
        //setPosition(symbol, 0f)
    }
}