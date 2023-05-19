package kaiex.exchange.simulator

import kaiex.exchange.simulator.adapters.TickAdapter
import kaiex.model.OrderSide
import kaiex.model.Trade
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TickPlayer(val fileName: String, val adapter: TickAdapter, val relativeTime: Boolean = true) {

    protected val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val MAX_TICKS = 30000

    fun start(): Flow<Trade> = flow {
        val file = File(Thread.currentThread().contextClassLoader.getResource(fileName).toURI())
        if (!file.exists()) {
            throw FileNotFoundException("Trade data file not found: $fileName")
        }

        log.info("Starting read of $fileName ($MAX_TICKS lines)")
        val reader = file.bufferedReader()

        reader.useLines { lines ->
            var lastTimestamp: Instant? = null
            var lineCount = 0

            val elapsed = measureTimeMillis {
                lines.forEachIndexed { index, line ->

                    // take every hundreth trade to keep voluem reasonable (not for actual back-testing!)
                    if (index % 100 == 0 && index <= MAX_TICKS) {

                        val trade = adapter.convert(line)
                        val delay = if (index == 0 || relativeTime == false) {
                            lastTimestamp = trade.createdAt
                            0
                        } else {
                            val relativeDelay = Duration.between(lastTimestamp!!, trade.createdAt)
                            relativeDelay.toMillis()
                        }
//                if (delay > 0) delay(delay)
//                    emit(trade)
//                    lastTimestamp = trade.createdAt

                        // tmp
                        //delay(5)
                        emit(trade)
                    }

                    lineCount++
                }
            }

            log.info("Processed $lineCount records in $elapsed ms")
        }
    }
}