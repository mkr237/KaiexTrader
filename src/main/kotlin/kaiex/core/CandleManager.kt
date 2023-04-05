package kaiex.core

import io.ktor.websocket.*
import kaiex.model.Candle
import kaiex.model.Trade
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.encodeToString as myJsonEncode

class CandleManager {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    var candleStart = Instant.now().truncatedTo(ChronoUnit.MINUTES)
    var candleOpen = 0.0f
    var candleHigh = 0.0f
    var candleLow = Float.MAX_VALUE
    var candleClose = 0.0f
    var tradeCount = 0
    var volume = 0.0f

    fun addTrade(trade: Trade): Candle {

        if (tradeCount == 0) {
            candleOpen = trade.price
        }
        candleHigh = max(candleHigh, trade.price)
        candleLow = min(candleLow, trade.price)
        candleClose = trade.price
        tradeCount++
        volume += trade.size

//        log.info("candleOpen: $candleOpen")
//        log.info("candleHigh: $candleHigh")
//        log.info("candleLow: $candleLow")
//        log.info("candleClose: $candleClose")
//        log.info("tradeCount: $tradeCount")
//        log.info("volume: $volume")

        return Candle(candleStart.epochSecond, candleOpen, candleHigh, candleLow, candleClose, tradeCount, volume)
    }

    fun subscribeCandles(): Flow<Candle> = flow {

        while (true) {
            // Wait until the next minute starts
            val nextMinute = candleStart.plusSeconds(60)
            val waitTime = nextMinute.toEpochMilli() - Instant.now().toEpochMilli()
            if (waitTime > 0) {
                delay(waitTime)
            }

            // Emit completed candle and reset
            val candle = Candle(candleStart.epochSecond, candleOpen, candleHigh, candleLow, candleClose, tradeCount, volume)
            emit(candle)
            candleStart = nextMinute
            candleOpen = candle.close
            candleHigh = candle.close
            candleLow = candle.close
            candleClose = candle.close
            tradeCount = 0
            volume = 0.0f
        }
    }
}
