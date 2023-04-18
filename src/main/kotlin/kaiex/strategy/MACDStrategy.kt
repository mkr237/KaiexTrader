package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.*
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyChartConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 9): Strategy("MACDStrategy:$symbol,$fastPeriod,$slowPeriod,$signalPeriod") {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName + ":" + strategyId)

    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)
    private val maxLong = 0.002f
    private val maxShort = -0.002f

    private var lastCandle: Long? = null
    override val config = StrategyChartConfig(
        strategyId = strategyId,
        strategyName = javaClass.simpleName,
        strategyDescription = "",
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

        // BUY/SELL every 30 seconds
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            var s = OrderSide.BUY
            var p = 31000f
            override fun run() {
                log.info("SENDING $s ORDER @ $p")
                createOrder(p, s, 0.001f)
                p = if (p == 31000f) 29000f else 31000f
                s = if (s == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
            }
        }, 0, 30000)
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
                    //createOrder(candle.close, OrderSide.BUY, maxLong - currentPosition)
                } else {
                    log.info("Already at or above MAX_LONG")
                }

            } else if (!candle.historical && macdLine < signalLine) {
                log.info("Detected SHORT signal - MACD: $macdLine < Signal: $signalLine\"")
                val currentPosition = getCurrentPosition(symbol)
                if(currentPosition > maxShort) {
                    log.info("Current position: $currentPosition - SELLING ${abs(maxShort - currentPosition)}")
                    //createOrder(candle.close, OrderSide.SELL, abs(maxShort - currentPosition))
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
