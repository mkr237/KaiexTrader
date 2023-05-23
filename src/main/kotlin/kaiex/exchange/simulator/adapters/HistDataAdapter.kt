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
        val bidPrice = fields[1].toBigDecimal()
        val askPrice = fields[2].toBigDecimal()
        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL,
            size = Random.nextFloat().toBigDecimal() * 1000.0.toBigDecimal(),
            price = (bidPrice * 20000.0.toBigDecimal()),
            createdAt = LocalDateTime.parse(timestampStr, formatter).atZone(ZoneId.systemDefault()).toInstant(),
            receivedAt = Instant.now(),
            liquidation = false,
            historical = false
        )
    }
}