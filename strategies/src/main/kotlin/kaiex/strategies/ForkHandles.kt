package kaiex.strategies

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.strategy.KaiexBaseStrategy
import kaiex.strategy.StrategyParams
import kaiex.ui.LineShape
import kaiex.ui.SeriesColor
import kaiex.ui.createChart

class ForkHandles(private val parameters: Map<String, String>): KaiexBaseStrategy() {

    // extract params
    private val strategyParams = StrategyParams(parameters, setOf("symbol"))
    private val symbol = strategyParams.getString("symbol")
    private val entryThreshold = 6
    private val exitThreshold = 20

    private var entryCount = 0
    private var exitCount = 0
    private val positionSize = 0.02

    private val chart = createChart("Default") {
        plot("Trade Data") {
            height = 0.6
            candleSeries("Candles") {
                upColor = SeriesColor.GREEN.rgb
                downColor = SeriesColor.RED.rgb
            }
        }
        plot("Signals") {
            height = 0.2
            lineSeries("Entry Count") {
                color = SeriesColor.BLUE.rgb
                shape = LineShape.hv
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
        addChart(chart)
    }

    override fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {

        log.debug("onMarketData: $snapshot")

        val candle = snapshot[symbol]?.lastCandle
        if (candle?.complete == true) {

            // update count
            if (candle.isGreen() && entryCount >= 0) entryCount++
            else if (candle.isRed() && entryCount <= 0) entryCount--
            else entryCount = 0

            // update chart
            chart.update(candle.startTimestamp.toEpochMilli()) {
                set(
                    "Candles",
                    listOf(
                        candle.open.toDouble(),
                        candle.high.toDouble(),
                        candle.low.toDouble(),
                        candle.close.toDouble()
                    )
                )
                set("Entry Count", entryCount)
                set("Position", getCurrentPosition(symbol))
            }

            //
            if(isFlat(symbol)) {
                if (entryCount <= -entryThreshold) {
                    log.info("Detected LONG signal at ${candle.startTimestamp} - Count: $entryCount")
                    setPosition(symbol, positionSize)
                    //count = 0
                } else if (entryCount >= entryThreshold) {
                    log.info("Detected SHORT signal at at ${candle.startTimestamp} - Count: $entryCount")
                    setPosition(symbol, -positionSize)
                    //count = 0
                }
            } else {
                if(++exitCount >= exitThreshold) {
                    setPosition(symbol, 0.0)
                    exitCount = 0
                }
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