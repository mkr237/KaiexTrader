package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.Candle
import kaiex.model.OrderSide
import kaiex.model.OrderTimeInForce
import kaiex.model.OrderType
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.abs

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 9): Strategy(
    strategyId = "MACDStrategy:$symbol,$fastPeriod,$slowPeriod,$signalPeriod",
    symbols = listOf(symbol),
    parameters = mapOf("fastPeriod" to fastPeriod.toString(), "slowPeriod" to slowPeriod.toString(), "signalPeriod" to signalPeriod.toString())) {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName + ":" + strategyId)

    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)
    private val maxLong = 0.002f
    private val maxShort = -0.002f

    private var lastCandle: Long? = null
    override val config = StrategyConfig(
        strategyId = strategyId,
        strategyType = javaClass.simpleName,
        strategyDescription = "",
        symbols = symbols,
        parameters = parameters,
        chartConfig = listOf(
            ChartSeriesConfig("price", "candle", 0, "#00FF00"),
            ChartSeriesConfig("histogram", "histogram", 1, "#26a69a"),
            ChartSeriesConfig("macd", "line", 1, "#2196F3"),
            ChartSeriesConfig("signal", "line", 1, "#FC6C02")
        )
    )

    override fun onStart() {
        log.info("onStart()")
        subscribeCandles(symbol) { candle -> onNewCandle(candle) }
    }

    override fun onUpdate() {
        log.info("onUpdate()")
    }

    override fun onStop() {
        log.info("onStop()")
    }

    private fun onNewCandle(candle: Candle) {
        log.info("Received Candle: $candle")

        if(candle.startTimestamp != lastCandle) {
            macd.update(candle.close.toDouble())
            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()

            if (!candle.historical && macdLine > signalLine) {
                log.info("Detected LONG signal - MACD: $macdLine > Signal: $signalLine")
                val currentPosition = getCurrentPosition(symbol)
                if(currentPosition < maxLong) {
                    log.info("Current position: $currentPosition - BUYING ${maxLong - currentPosition}")
                    createOrder(candle.close, OrderSide.BUY, maxLong - currentPosition)
                } else {
                    log.info("Already at or above MAX_LONG")
                }

            } else if (!candle.historical && macdLine < signalLine) {
                log.info("Detected SHORT signal - MACD: $macdLine < Signal: $signalLine\"")
                val currentPosition = getCurrentPosition(symbol)
                if(currentPosition > maxShort) {
                    log.info("Current position: $currentPosition - SELLING ${abs(maxShort - currentPosition)}")
                    createOrder(candle.close, OrderSide.SELL, abs(maxShort - currentPosition))
                } else {
                    log.info("Already at or below MAX_SHORT")
                }
            }

            val updates = listOf(
                SeriesUpdate.NumericUpdate(id = "macd", value = macdLine),
                SeriesUpdate.NumericUpdate(id = "signal", value = signalLine),
                SeriesUpdate.NumericUpdate(id = "histogram", value = histogram)
            )

            sendStrategyMarketDataUpdate(candle.lastUpdate, updates)
            lastCandle = candle.startTimestamp
        }

        val updates = listOf(SeriesUpdate.CandleUpdate("price", candle.open.toDouble(), candle.high.toDouble(),
            candle.low.toDouble(), candle.close.toDouble()))

        sendStrategyMarketDataUpdate(candle.lastUpdate, updates)
    }

    private fun createOrder(price: Float, side: OrderSide, size: Float) {
        createOrder(
            symbol,
            OrderType.MARKET,
            side,
            price,
            size,
            0.015f,
            OrderTimeInForce.FOK
        )
    }
}
