package kaiex.strategy

import kaiex.util.EventBroadcaster
import kaiex.indicator.MACD
import kaiex.model.*
import kaiex.util.UUID5
import kaiex.util.WebSocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.serialization.encodeToString as myJsonEncode

@Serializable
data class TradeData(val time: Long, val price: Double, val size: Double, val macd: Double, val signal: Double)

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 12): Strategy("MACDStrategy/$symbol/$fastPeriod/$slowPeriod/$signalPeriod") {

    private val log: Logger = LoggerFactory.getLogger(strategyId)
    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)

    private val positionSize = BigDecimal.valueOf(0.01)
    private val positionSize2 = BigDecimal.valueOf(0.02)
    private var position = BigDecimal.ZERO

    suspend fun start() {

        val tradeBroadcaster: EventBroadcaster<Trade> = marketDataManager.subscribeTrades(symbol)
        tradeBroadcaster.listenForEvents().collect { trade ->

            // Update the MACD with the latest trade price
            macd.update(trade.price.toDouble())

            // Print the MACD values
            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()
            log.info("MACD line: $macdLine, Signal line: $signalLine, Histogram: $histogram")

            //
            val newOrders = mutableListOf<Order>()
            if (macdLine > signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    newOrders.add(createOrder(Side.BUY, trade.price, positionSize.toFloat()))
                    position += positionSize
                } else if(position < BigDecimal.ZERO) {
                    newOrders.add(createOrder(Side.BUY, trade.price, positionSize2.toFloat()))
                    position += positionSize2
                }
            } else if (macdLine < signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    newOrders.add(createOrder(Side.SELL, trade.price, positionSize.toFloat()))
                    position -= positionSize
                } else if(position > BigDecimal.ZERO) {
                    newOrders.add(createOrder(Side.SELL, trade.price, positionSize2.toFloat()))
                    position -= positionSize2
                }
            }

            // create and report a strategy snapshot
            val snapshot = StrategySnapshot()
            snapshot.strategyId = strategyId
            snapshot.pnl = 0.0
            snapshot.orders = newOrders
            snapshot.positions = emptyList()
            snapshot.timeStamp = trade.createdAt.epochSecond
            snapshot.marketData = mapOf("price" to trade.price.toDouble(),
                                        "macd" to macdLine,
                                        "signal" to signalLine,
                                        "histogram" to histogram)

            reportManager.submitStrategyReport(snapshot)
        }

//        val orderBookBroadcaster: EventBroadcaster<OrderBook> = marketDataManager.subscribeOrderBook(symbol)
//        orderBookBroadcaster.listenForEvents().collect { ob ->
//            log.info("Received Order Book: $ob")
//
//            val bestBid = ob.bids[0].price.toDouble()
//            val bestAsk = ob.asks[0].price.toDouble()
//            val midPrice = bestBid + ((bestAsk - bestBid) / 2)
//
//            // Update the MACD with the latest trade price
//            macd.update(midPrice)
//
//            // Print the MACD values
//            val macdLine = macd.getMACDLine()
//            val signalLine = macd.getSignalLine()
//            val histogram = macd.getHistogram()
//            log.info("MACD line: $macdLine, Signal line: $signalLine, Histogram: $histogram")
//
//            // create and report a strategy snapshot
//            val snapshot = StrategySnapshot()
//            snapshot.strategyId = strategyId
//            snapshot.pnl = 0.0
//            snapshot.orders = emptyList()
//            snapshot.positions = emptyList()
//            snapshot.timeStamp = ob.receivedAt.epochSecond
//            snapshot.marketData = mapOf("bestBid" to bestBid,
//                                        "bestAsk" to bestAsk,
//                                        "midPrice" to midPrice,
//                                        "macd" to macdLine,
//                                        "signal" to signalLine,
//                                        "histogram" to histogram)
//
//            reportManager.submitStrategyReport(snapshot)
//        }
    }

    private fun createOrder(side: Side, price: Float, size: Float):Order {
        log.info("Creating $side order ($size @ $price)")
        return Order(
            UUID.randomUUID().toString(),
            "DYDX",
            symbol,
            Type.MARKET,
            side,
            price,
            size,
            OrderStatus.PENDING)
    }
}

