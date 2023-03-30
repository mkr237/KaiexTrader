package kaiex.strategy

import kaiex.indicator.MACD
import kaiex.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*
import kotlin.collections.HashMap

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

    private var md:MutableMap<String, Double> = mutableMapOf()

    suspend fun start() {

        coroutineScope {
            async {
                marketDataManager.subscribeTrades(symbol).listenForEvents().collect { trade -> handleTradeEvent(trade) }
            }

            async {
                marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { ob -> handleOrderBookUpdate(ob) }
            }
        }
    }

    private fun handleTradeEvent(trade: Trade) {

        log.info("Received Trade: $trade")

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

        // update market data
        md["price"] = trade.price.toDouble()
        md["macd"] = macdLine
        md["signal"] = signalLine
        md["histogram"] = histogram

        // create and report a strategy snapshot
        var snapshot = StrategyReport(strategyId)
        snapshot.orders = ArrayList(newOrders)
        snapshot.timeStamp = trade.createdAt.epochSecond
        snapshot.marketData = HashMap(md)
        reportManager.submitStrategyReport(snapshot)
    }

    private fun handleOrderBookUpdate(ob: OrderBook) {
        //log.info("Received Order Book: $ob")

        val bestBid = ob.bids[0].price.toDouble()
        val bestAsk = ob.asks[0].price.toDouble()
        val midPrice = bestBid + ((bestAsk - bestBid) / 2)

        // update market data
        md["bestBid"] = bestBid
        md["bestAsk"] = bestAsk
        md["midPrice"] = midPrice

        // create and report a strategy snapshot
        var snapshot = StrategyReport(strategyId)
        snapshot.orders = mutableListOf()
        snapshot.timeStamp = ob.receivedAt.epochSecond      // TODO different to createdAt as per trades
        snapshot.marketData = HashMap(md)
        reportManager.submitStrategyReport(snapshot)
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

