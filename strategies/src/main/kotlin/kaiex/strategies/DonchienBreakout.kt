package kaiex.strategies

import kaiex.indicator.DonchianChannel
import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.strategy.KaiexBaseStrategy
import kaiex.strategy.StrategyParams
import kaiex.ui.LineShape
import kaiex.ui.SeriesColor
import kaiex.ui.createChart

class DonchienBreakout(private val parameters: Map<String, String>): KaiexBaseStrategy() {

    // extract params
    private val strategyParams = StrategyParams(parameters, setOf("symbol"))
    private val symbol = strategyParams.getString("symbol")
    private val lookbackPeriod = strategyParams.getInt("lookback")

    private val donchienChannel = DonchianChannel(lookbackPeriod)
    private val positionSize = 0.02

    private val chart = createChart("Default") {
        plot("Trade Data") {
            height = 0.6
            candleSeries("Candles") {
                upColor = SeriesColor.GREEN.rgb
                downColor = SeriesColor.RED.rgb
            }
            lineSeries("Upper") {
                color = SeriesColor.ORANGE.rgb
            }
            lineSeries("Lower") {
                color = SeriesColor.BLUE.rgb
            }
        }
        plot("Postion") {
            height = 0.4
            lineSeries("Position") {
                color = SeriesColor.GREY.rgb
                shape = LineShape.hv
            }
        }
    }

    override fun onCreate() {
        log.info("onCreate: $parameters")
        addSymbol(symbol)
        //addIndicator("DU", symbol, donchienUpper)
        addChart(chart)
    }

    override fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {

        log.debug("onMarketData: $snapshot")

        val candle = snapshot[symbol]?.lastCandle
        if (candle?.complete == true) {

            donchienChannel.update(candle)

            val donchienUpper = donchienChannel.getUpper()
            val donchienLower = donchienChannel.getLower()

//            if (!candle.historical && macdLine > signalLine) {
//                log.info("Detected LONG signal - MACD: $macdLine > Signal: $signalLine")
//                setPosition(symbol, positionSize)
//            } else if (!candle.historical && macdLine < signalLine) {
//                log.info("Detected SHORT signal - MACD: $macdLine < Signal: $signalLine\"")
//                setPosition(symbol, -positionSize)
//            }

            // update chart
            chart.update(candle.startTimestamp.toEpochMilli()) {
                set("Candles", listOf(candle.open.toDouble(), candle.high.toDouble(), candle.low.toDouble(), candle.close.toDouble()))
                set("Upper", donchienUpper ?: candle.close)
                set("Lower", donchienLower ?: candle.close)
                set("Position", getCurrentPosition(symbol))
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