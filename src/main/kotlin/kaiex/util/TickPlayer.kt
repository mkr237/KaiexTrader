package kaiex.util

import kaiex.model.Side
import kaiex.model.Trade
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

interface TickAdapter {
    fun convert(line: String):Trade
}

/**
 * Reads data as provided at http://www.histdata.com/download-free-forex-data/
 */
class HistDataAdapter(val symbol: String) : TickAdapter {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmssSSS")
    override fun convert(line: String):Trade {
        val fields = line.split(",")
        val timestampStr = fields[0]
        val bidPrice = fields[1].toFloat()
        val askPrice = fields[2].toFloat()
        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) Side.BUY else Side.SELL,
            size = Random.nextFloat() * 1000,
            price = (bidPrice * 20000f),
            createdAt = LocalDateTime.parse(timestampStr, formatter).atZone(ZoneId.systemDefault()).toInstant(),
            receivedAt = Instant.now(),
            liquidation = false
        )
    }
}

class BinanceAdapter(val symbol: String) : TickAdapter {
    private val formatter = DateTimeFormatter.ofPattern("")
    override fun convert(line: String):Trade {
        val fields = line.split(",")
        val tradeId = fields[0]
        val price = fields[1].toFloat()
        val size = fields[2].toFloat()
        val quoteSize = fields[3].toFloat()
        val timestamp = fields[4] //.toLong() / 1000L
        val isBuyerMaker = fields[5].toBoolean()
        val isBestMatch = fields[6].toBoolean()

        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) Side.BUY else Side.SELL,
            size = size,
            price = price,
            createdAt = Instant.ofEpochMilli(timestamp.toLong()).truncatedTo(ChronoUnit.SECONDS),
            receivedAt = Instant.now(),
            liquidation = false
        )
    }
}

class TickPlayer(val fileName: String, val adapter: TickAdapter, val relativeTime: Boolean = true) {
    fun start(): Flow<Trade> = flow {
        val file = File(Thread.currentThread().contextClassLoader.getResource(fileName).toURI())
        if (!file.exists()) {
            throw FileNotFoundException("Trade data file not found: $fileName")
        }
        val reader = file.bufferedReader()
        reader.useLines { lines ->
            var lastTimestamp: Instant? = null
            lines.forEachIndexed { index, line ->
                val trade = adapter.convert(line)
                val delay = if (index == 0 || relativeTime == false) {
                    lastTimestamp = trade.createdAt
                    0
                } else {
                    val relativeDelay = Duration.between(lastTimestamp!!, trade.createdAt)
                    relativeDelay.toMillis()
                }
                if (delay > 0) delay(delay)
                    emit(trade)
                    lastTimestamp = trade.createdAt
            }
        }
    }
}