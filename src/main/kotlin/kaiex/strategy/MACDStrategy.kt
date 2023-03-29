package kaiex.strategy

import kaiex.util.EventBroadcaster
import kaiex.indicator.MACD
import kaiex.model.*
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

@Serializable
data class TradeData(val time: Long, val price: Double, val size: Double, val macd: Double, val signal: Double)

class MACDStrategy(val symbol: String,
                   val fastPeriod:Int = 12,
                   val slowPeriod:Int = 26,
                   val signalPeriod:Int = 12): Strategy("MACDStrategy/$symbol/$fastPeriod/$slowPeriod/$signalPeriod") {

    private val log: Logger = LoggerFactory.getLogger(strategyId)
    private val macd = MACD(fastPeriod, slowPeriod, signalPeriod)

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

            // Generate trading signals based on the MACD strategy
            if (macdLine > signalLine) {
                log.info("BUY")
            } else if (macdLine < signalLine) {
                log.info("SELL")
            } else {
                log.info("HOLD")
            }

            val snapshot = StrategySnapshot()
            snapshot.strategyId = strategyId
            snapshot.pnl = 0.0
            snapshot.orders = emptyList()
            snapshot.positions = emptyList()
            snapshot.timeStamp = trade.createdAt.epochSecond
            snapshot.marketData = mapOf("price" to trade.price.toDouble(),
                                        "macd" to macdLine,
                                        "signal" to signalLine,
                                        "histogram" to histogram)

            reportManager.submitStrategyReport(snapshot)
        }
    }
}

