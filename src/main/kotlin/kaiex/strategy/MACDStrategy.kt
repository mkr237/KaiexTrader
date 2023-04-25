package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderSide
import kaiex.model.OrderUpdate
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyConfig
import kaiex.ui.StrategyMarketDataUpdate
import kotlinx.coroutines.delay
import kotlin.math.abs

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
    private var lastCandle: Long? = null

    override fun onStrategyCreate() {
        log.info("onStrategyCreate()")
        symbol = config.symbols[0]
    }

    override fun onStrategyMarketData(snapshot: MarketDataSnapshot) {
        //log.info("Candle: ${snapshot.getCandles(symbol!!).lastOrNull() ?: "-"}")

        val candle = snapshot.getCandles(symbol!!).lastOrNull()

        if(candle != null && candle.startTimestamp != lastCandle) {

            macd.update(candle.close.toDouble())
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
            lastCandle = candle.startTimestamp
        }
    }

    override fun onStrategyOrderUpdate(update: OrderUpdate) {
        log.info("*** Received order update ***")
        log.info("Order Update: $update")
    }

    override fun onStrategyDestroy() {
        log.info("onStop()")
    }
}