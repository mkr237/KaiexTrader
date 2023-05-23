package kaiex.exchange.simulator.adapters

import kaiex.model.OrderSide
import kaiex.model.Trade
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Reads tick data as provided at https://data.binance.vision/?prefix=data/spot/daily/trades/
 */
class BinanceAdapter(val symbol: String) : TickAdapter {
    private val formatter = DateTimeFormatter.ofPattern("")
    override fun convert(line: String): Trade {
        val fields = line.split(",")
        val tradeId = fields[0]
        val price = fields[1].toBigDecimal()
        val size = fields[2].toBigDecimal()
        val quoteSize = fields[3].toBigDecimal()
        val timestamp = Instant.ofEpochMilli(fields[4].toLong())
        val isBuyerMaker = fields[5].toBoolean()
        val isBestMatch = fields[6].toBoolean()

        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL,
            size = size,
            price = price,
            createdAt = timestamp,
            receivedAt = Instant.now(),
            liquidation = false,
            historical = false
        )
    }
}