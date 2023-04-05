package kaiex.strategy

import kaiex.core.format
import kaiex.indicator.MACD
import kaiex.model.Candle
import kaiex.model.Order
import kaiex.model.Trade
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.encodeToString as myJsonEncode

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 9): Strategy("MACDStrategy/$symbol/$fastPeriod/$slowPeriod/$signalPeriod") {

    @Serializable
    private data class Update(val timestamp:Long,
                              val open: Double,
                              val high: Double,
                              val low: Double,
                              val close: Double,
                              val totalVolume: Double,
                              val numTrades: Int,
                              val macd:Double,
                              val signal:Double,
                              val histgram:Double)

    private val log: Logger = LoggerFactory.getLogger(strategyId)
    private val uiServer : UIServer by inject()

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
                marketDataManager.subscribeCandles(symbol).listenForEvents().collect { candle -> handleCandleEvent(candle) }
            }

            uiServer.createSocket("/$strategyId")
        }
    }

    private fun handleTradeEvent(trade: Trade) {
        log.info("Received Trade: $trade")
    }

    private fun handleCandleEvent(candle: Candle) {
        //log.info("Received Candle: $candle")

        // Update the MACD with the latest trade price
        macd.update(candle.close.toDouble())

        //
        val macdLine = macd.getMACDLine()
        val signalLine = macd.getSignalLine()
        val histogram = macd.getHistogram()

        //
        val newOrders = mutableListOf<Order>()
        if (macdLine > signalLine) {
            if(position.equals(BigDecimal.ZERO)) {
                //newOrders.add(createOrder(Side.BUY, candle.close, positionSize.toFloat()))
                position += positionSize
            } else if(position < BigDecimal.ZERO) {
                //newOrders.add(createOrder(Side.BUY, candle.close, positionSize2.toFloat()))
                position += positionSize2
            }
        } else if (macdLine < signalLine) {
            if(position.equals(BigDecimal.ZERO)) {
                //newOrders.add(createOrder(Side.SELL, candle.close, positionSize.toFloat()))
                position -= positionSize
            } else if(position > BigDecimal.ZERO) {
                //newOrders.add(createOrder(Side.SELL, candle.close, positionSize2.toFloat()))
                position -= positionSize2
            }
        }

        // update market data
        val update = Update(
            candle.startTimestamp,
            candle.open.toDouble(),
            candle.high.toDouble(),
            candle.low.toDouble(),
            candle.close.toDouble(),
            candle.volume.toDouble(),
            candle.numTrades,
            macdLine,
            signalLine,
            histogram)

        log.info(update.toString())
        uiServer.sendData("/$strategyId", DataPacket(0, format.myJsonEncode(update)))
    }

//    private fun createOrder(side: Side, price: Float, size: Float):Order {
//        log.info("Creating $side order ($size @ $price)")
//        return Order(
//            UUID.randomUUID().toString(),
//            "DYDX",
//            symbol,
//            Type.MARKET,
//            side,
//            price,
//            size,
//            OrderStatus.PENDING)
//    }
}

