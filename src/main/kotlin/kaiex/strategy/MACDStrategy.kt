package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 9): Strategy("MACDStrategy:$symbol,$fastPeriod,$slowPeriod,$signalPeriod") {

    private val log: Logger = LoggerFactory.getLogger(strategyId)

    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)

    private val positionSize = BigDecimal.valueOf(0.01)
    private val positionSize2 = BigDecimal.valueOf(0.02)
    private var position = BigDecimal.ZERO
    private var lastCandle: Long? = null
    override val config = StrategyChartConfig(
        strategyId = strategyId,
        strategyName = javaClass.simpleName,
        strategyDescription = "",
        chartConfig = listOf(
            ChartSeriesConfig(
                id = "price",
                seriesType = "candle",
                pane = 0,
                color = "#00FF00"
            ),
            ChartSeriesConfig(
                id = "histogram",
                seriesType = "histogram",
                pane = 1,
                color = "#26a69a"
            ),
            ChartSeriesConfig(
                id = "macd",
                seriesType = "line",
                pane = 1,
                color = "#2196F3"
            ),
            ChartSeriesConfig(
                id = "signal",
                seriesType = "line",
                pane = 1,
                color = "#FC6C02"
            )
        )
    )

    override suspend fun onStart() {
        log.info("onStart()")

        subscribeCandles(symbol) { candle -> handleCandleEvent(candle) }
    }

    override suspend fun onUpdate() {
        log.info("onUpdate()")
    }

    override suspend fun onStop() {
        log.info("onStop()")
    }

    private fun handleCandleEvent(candle: Candle) {
        log.info("Received Candle: $candle")

        if(candle.startTimestamp != lastCandle) {
            macd.update(candle.close.toDouble())
            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()

            var order:CreateOrder? = null
            if (macdLine > signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    order = createOrder(candle.startTimestamp, OrderSide.BUY, candle.close, positionSize.toFloat())
                    position += positionSize
                } else if(position < BigDecimal.ZERO) {
                    order = createOrder(candle.startTimestamp, OrderSide.BUY, candle.close, positionSize2.toFloat())
                    position += positionSize2
                }
            } else if (macdLine < signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    order = createOrder(candle.startTimestamp, OrderSide.SELL, candle.close, positionSize.toFloat())
                    position -= positionSize
                } else if(position > BigDecimal.ZERO) {
                    order = createOrder(candle.startTimestamp, OrderSide.SELL, candle.close, positionSize2.toFloat())
                    position -= positionSize2
                }
            }

            val updates = listOf(
                SeriesUpdate.NumericUpdate(
                    id = "macd",
                    value = macdLine
                ),
                SeriesUpdate.NumericUpdate(
                    id = "signal",
                    value = signalLine
                ),
                SeriesUpdate.NumericUpdate(
                    id = "histogram",
                    value = histogram
                )
            )

            uiServer.send(StrategyMarketDataUpdate(strategyId, candle.lastUpdate, updates))
            lastCandle = candle.startTimestamp
        }

        val updates = listOf(
            SeriesUpdate.CandleUpdate(
                id = "price",
                open = candle.open.toDouble(),
                high = candle.high.toDouble(),
                low = candle.low.toDouble(),
                close = candle.close.toDouble()
            )
        )
        uiServer.send(StrategyMarketDataUpdate(strategyId, candle.startTimestamp, updates))
    }

    private fun createOrder(time: Long, side: OrderSide, price: Float, size: Float):CreateOrder {
        log.info("Creating $side order ($size @ $price)")
        return CreateOrder(
            UUID.randomUUID().toString(),
            "DYDX",
            symbol,
            OrderType.MARKET,
            side,
            price,
            size,
            OrderTimeInForce.GTT,
            false,
            time)
    }
}
