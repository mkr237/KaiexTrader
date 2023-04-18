package kaiex.strategy

import kaiex.model.Candle
import kaiex.model.OrderSide
import kaiex.model.OrderTimeInForce
import kaiex.model.OrderType
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyChartConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class DumbStrategy(val symbol: String,
                   val period:Long = 30000): Strategy("DumbStrategy:$symbol,$period") {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName + ":" + strategyId)

    override val config = StrategyChartConfig(
        strategyId = strategyId,
        strategyName = javaClass.simpleName,
        strategyDescription = "",
        chartConfig = listOf(
            ChartSeriesConfig("price", "candle", 0, "#00FF00")
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
