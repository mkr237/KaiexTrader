package kaiex.strategy

import kaiex.core.format
import kaiex.indicator.MACD
import kaiex.model.*
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.math.BigDecimal
import java.util.*
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
    private var lastCandle: Long? = null

    suspend fun start() {

        coroutineScope {
//            async {
//                marketDataManager.subscribeTrades(symbol).listenForEvents().toCandles().collect { candle -> handleCandleEvent(candle) }
//            }

            async {
                accountManager.subscribeAccountUpdates("0").listenForEvents().collect { update -> handleAccountUpdate(update) }
            }

            uiServer.createSocket("/$strategyId")
        }
    }

    private fun handleCandleEvent(candle: Candle) {
        log.info("Received Candle: $candle")

        val routeId = "/$strategyId"

        if(candle.startTimestamp != lastCandle) {
            macd.update(candle.close.toDouble())
            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()
            val update = MACDUpdate(candle.startTimestamp, macdLine, signalLine, histogram)
            println(update)

            var order:Order? = null
            if (macdLine > signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    order = createOrder(candle.startTimestamp, Side.BUY, candle.close, positionSize.toFloat())
                    position += positionSize
                } else if(position < BigDecimal.ZERO) {
                    order = createOrder(candle.startTimestamp, Side.BUY, candle.close, positionSize2.toFloat())
                    position += positionSize2
                }
            } else if (macdLine < signalLine) {
                if(position.equals(BigDecimal.ZERO)) {
                    order = createOrder(candle.startTimestamp, Side.SELL, candle.close, positionSize.toFloat())
                    position -= positionSize
                } else if(position > BigDecimal.ZERO) {
                    order = createOrder(candle.startTimestamp, Side.SELL, candle.close, positionSize2.toFloat())
                    position -= positionSize2
                }
            }

            if(order != null) {
                uiServer.sendData(routeId, DataPacket(0, format.myJsonEncode(order)))
                log.info("Placing Order: $order")
            }

            uiServer.sendData(routeId, DataPacket(0, format.myJsonEncode(update)))
            lastCandle = candle.startTimestamp
        }

        println(candle)
        uiServer.sendData(routeId, DataPacket(0, format.myJsonEncode(candle)))
    }

    private fun createOrder(time: Long, side: Side, price: Float, size: Float):Order {
        log.info("Creating $side order ($size @ $price)")
        return Order(
            UUID.randomUUID().toString(),
            "DYDX",
            symbol,
            Type.MARKET,
            side,
            price,
            size,
            OrderStatus.PENDING,
            time)
    }

    private fun handleAccountUpdate(update: AccountUpdate) {
        log.info("Received Account Update: $update")
    }
}
