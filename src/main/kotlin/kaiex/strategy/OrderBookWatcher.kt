package kaiex.strategy

import kaiex.model.OrderBook
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.StrategyConfig
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class OrderBookWatcher(val symbol: String): Strategy(
    strategyId = "OrderBookWatcher:$symbol",
    symbols = listOf(symbol),
    parameters = mapOf()) {

    private val log: Logger = LoggerFactory.getLogger(strategyId)
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

    @Serializable
    data class Update(val timestamp:Long, val bestBid:Double, val bestAsk:Double, val midPrice:Double)

    override fun onStart() {
        log.info("onStart()")
    }

    override fun onUpdate() {
        log.info("onUpdate()")
    }

    override fun onStop() {
        log.info("onStop()")
    }

    private fun handleOrderBookUpdate(ob: OrderBook) {
        //log.info("Received Order Book: $ob")

        val bestBid = ob.bids[0].price.toDouble()
        val bestAsk = ob.asks[0].price.toDouble()
        val midPrice = bestBid + ((bestAsk - bestBid) / 2)
        val update = OrderBookWatcher.Update(Instant.now().epochSecond, bestBid, bestAsk, midPrice)
        //log.info(update.toString())
        //uiServer.sendMessage("order_book", format.myJsonEncode(update))
    }
}