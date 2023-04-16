package kaiex.strategy

import kaiex.model.OrderBook
import kaiex.ui.UIServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class OrderBookWatcher(val symbol: String): Strategy("OrderBookWatcher:$symbol") {
    private val log: Logger = LoggerFactory.getLogger(strategyId)
    override val config = StrategyChartConfig(
        strategyId = strategyId,
        strategyName = javaClass.simpleName,
        strategyDescription = "",
        chartConfig = listOf(
            ChartSeriesConfig(
                id = "price",
                seriesType = "candle",
                pane = 0,
                color = "blue"
            ),
            ChartSeriesConfig(
                id = "macd",
                seriesType = "line",
                pane = 1,
                color = "green"
            ),
            ChartSeriesConfig(
                id = "signal",
                seriesType = "line",
                pane = 1,
                color = "red"
            ),
            ChartSeriesConfig(
                id = "histogram",
                seriesType = "histogram",
                pane = 1,
                color = "purple"
            )
        )
    )

    @Serializable
    data class Update(val timestamp:Long, val bestBid:Double, val bestAsk:Double, val midPrice:Double)

    override suspend fun onStart() {
        log.info("onStart()")
//        scope.async {
//            marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { ob -> handleOrderBookUpdate(ob) }
//        }
    }

    override suspend fun onUpdate() {
        log.info("onUpdate()")
    }

    override suspend fun onStop() {
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
        uiServer.send(
            StrategySnapshot(
                strategyId,
                Instant.now().epochSecond,
                0f,
                0,
                0f,
                0f,
                0f,
                emptyMap(),
                orders,
                fills,
                positions))
    }
}