package kaiex.exchange.simulator.adapters

import kaiex.model.OrderSide
import kaiex.model.Trade
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * Reads tick data as provided at http://www.histdata.com/download-free-forex-data/
 */
class HistDataAdapter(val symbol: String) : TickAdapter {

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmssSSS")
    override fun convert(line: String): Trade {
        val fields = line.split(",")
        val timestampStr = fields[0]
        val bidPrice = fields[1].toFloat()
        val askPrice = fields[2].toFloat()
        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL,
            size = Random.nextFloat() * 1000,
            price = (bidPrice * 20000f),
            createdAt = LocalDateTime.parse(timestampStr, formatter).atZone(ZoneId.systemDefault()).toInstant(),
            receivedAt = Instant.now(),
            liquidation = false,
            historical = false
        )
    }
}