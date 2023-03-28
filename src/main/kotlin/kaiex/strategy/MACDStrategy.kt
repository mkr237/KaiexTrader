package kaiex.strategy

import kaiex.core.EventBroadcaster
import kaiex.indicator.MACD
import kaiex.model.Trade
import kaiex.util.WebSocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.serialization.encodeToString as myJsonEncode

val messageQueue = LinkedBlockingQueue<String>()

@Serializable
data class TradeData(val time: Long, val price: Double, val size: Double, val macd: Double, val signal: Double)

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 12): Strategy() {

    private val log: Logger =
        LoggerFactory.getLogger("${javaClass.simpleName}($symbol,$fastPeriod,$slowPeriod,$signalPeriod)")

    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)

    suspend fun start() {

        WebSocketServer.startServer()

        // tmp
        delay(2000)

        // start a coroutine to take flush the queue out to ws clients
        GlobalScope.launch {
            while (true) {
                if(WebSocketServer.isConnected()) {
                    val message = messageQueue.take()
                    WebSocketServer.sendDataToSocket(message)
                }
            }
        }

        val tradeBroadcaster: EventBroadcaster<Trade> = md.subscribeTrades(symbol)
        tradeBroadcaster.listenForEvents().collect { trade ->

            // Update the MACD with the latest trade price
            macd.update(trade.price.toDouble())

            // Print the MACD values
            val macdLine = macd.getMACDLine()
            val signalLine = macd.getSignalLine()
            val histogram = macd.getHistogram()
            log.info("MACD line: $macdLine, Signal line: $signalLine, Histogram: $histogram")

            // Generate trading signals based on the MACD strategy
            if (macdLine > signalLine) {
                log.info("BUY")
            } else if (macdLine < signalLine) {
                log.info("SELL")
            } else {
                log.info("HOLD")
            }

            val data = TradeData(
                trade.createdAt.epochSecond,
                trade.price.toDouble(),
                trade.size.toDouble(),
                macdLine,
                signalLine
            )

            messageQueue.put(Json.myJsonEncode(data))
        }
    }
}

