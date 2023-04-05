package kaiex.strategy

import kaiex.core.format
import kaiex.model.OrderBook
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlinx.serialization.encodeToString as myJsonEncode

class OrderBookWatcher(val symbol: String): Strategy("OrderBookWatcher/$symbol") {
    private val log: Logger = LoggerFactory.getLogger(strategyId)
    private val uiServer : UIServer by inject()

    @Serializable
    data class Update(val timestamp:Long, val bestBid:Double, val bestAsk:Double, val midPrice:Double)

    suspend fun start() {

        coroutineScope {
            async {
                marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { ob -> handleOrderBookUpdate(ob) }
            }

            uiServer.createSocket("/$strategyId")
        }
    }

    private fun handleOrderBookUpdate(ob: OrderBook) {
        //log.info("Received Order Book: $ob")

        val bestBid = ob.bids[0].price.toDouble()
        val bestAsk = ob.asks[0].price.toDouble()
        val midPrice = bestBid + ((bestAsk - bestBid) / 2)
        val update = OrderBookWatcher.Update(Instant.now().epochSecond, bestBid, bestAsk, midPrice)
        //log.info(update.toString())
        uiServer.sendData("/$strategyId", DataPacket(0, format.myJsonEncode(update)))
    }
}