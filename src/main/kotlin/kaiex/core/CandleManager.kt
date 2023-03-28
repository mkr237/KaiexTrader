package kaiex.core

import io.ktor.websocket.*
import kaiex.model.Candle
import kaiex.model.Trade
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlinx.serialization.encodeToString as myJsonEncode

class CandleManager {
    private var currentCandle: Candle? = null
    private var lastCandle: Candle? = null
    private var currentCandleStartTime: Long = 0

    suspend fun addTrade(trade: Trade) {
        val tradeTimestamp = trade.createdAt.toEpochMilli()
        val candleStartTime = tradeTimestamp - tradeTimestamp % (60 * 1000)

        if (currentCandle == null || currentCandleStartTime != candleStartTime) {
            //finalizeCurrentCandle()
            currentCandle = Candle(
                startTimestamp = Instant.ofEpochMilli(candleStartTime),
                open = trade.price,
                high = trade.price,
                low = trade.price,
                close = trade.price,
                volume = trade.size
            )
            currentCandleStartTime = candleStartTime
        } else {
            currentCandle!!.close = trade.price
            currentCandle!!.volume += trade.size
            if (trade.price > currentCandle!!.high) {
                currentCandle!!.high = trade.price
            }
            if (trade.price < currentCandle!!.low) {
                currentCandle!!.low = trade.price
            }
        }
    }

    fun printCandle(): Candle? {
        if(currentCandle != null) {
            lastCandle = currentCandle
            currentCandle = null
            return lastCandle
        }

        return lastCandle
    }
}
