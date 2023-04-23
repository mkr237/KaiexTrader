package kaiex.strategy

import kaiex.model.*
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.SeriesUpdate
import kaiex.ui.StrategyChartConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.abs

class DumbStrategy(val symbol: String, period:Long = 30000): Strategy(
    strategyId = "DumbStrategy:$symbol,$period",
    symbols = listOf(symbol),
    parameters = mapOf("period" to period.toString())) {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName + ":" + strategyId)
    var lastPrice:Float? = null
    private val maxLong = 0.02f
    private val maxShort = -0.02f

    override val config = StrategyChartConfig(
        strategyId = strategyId,
        strategyType = javaClass.simpleName,
        strategyDescription = "",
        symbols = symbols,
        parameters = parameters,
        chartConfig = listOf(
            ChartSeriesConfig("price", "candle", 0, "#00FF00"),
            ChartSeriesConfig("best-bid", "line", 0, "#2196F3"),
            ChartSeriesConfig("best-ask", "line", 0, "#FC6C02")
        )
    )

    override fun onStart() {
        log.info("onStart()")

        subscribeCandles(symbol) { candle -> onNewCandle(candle) }
        //subscribeOrderBook(symbol) { update -> onOrderBookUpdate(update) }

        // BUY/SELL every [period]] seconds
        val timer = Timer()

        // one-shot
        timer.schedule(object : TimerTask() {
            override fun run() {

                val size = maxLong - getCurrentPosition(symbol)
                log.info("BUYING $size")
                createOrder(35000f, OrderSide.BUY, size)
            }
        }, 10000)

        // repeating
        timer.scheduleAtFixedRate(object : TimerTask() {
            var isLong = true
            override fun run() {

//                // check we have a price
//                if(lastPrice == null) {
//                    log.info("NO PRICE - WILL WAIT")
//                    return
//                }
//
//                if(isLong) {
//                    val size = maxLong - getCurrentPosition(symbol)
//                    log.info("BUYING $size")
//                    createOrder(35000f, OrderSide.BUY, size)
//                } else {
//                    val size = abs(maxShort - getCurrentPosition(symbol))
//                    log.info("SELLING $size")
//                    createOrder(25000f, OrderSide.SELL, size)
//                }

                isLong = !isLong
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

        lastPrice = candle.close

        val updates = listOf(SeriesUpdate.CandleUpdate("price", candle.open.toDouble(), candle.high.toDouble(),
            candle.low.toDouble(), candle.close.toDouble()))

        sendStrategyMarketDataUpdate(candle.lastUpdate, updates)
    }

    private fun onOrderBookUpdate(ob: OrderBook) {
        log.info("Received Order Book: $ob")

        val bestBid = ob.bids[0].price.toDouble()
        val bestAsk = ob.asks[0].price.toDouble()
        val midPrice = bestBid + ((bestAsk - bestBid) / 2)
        val updates = listOf(SeriesUpdate.NumericUpdate("best-bid", bestBid), SeriesUpdate.NumericUpdate("best-ask", bestAsk))
        sendStrategyMarketDataUpdate(ob.receivedAt.epochSecond, updates)
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
