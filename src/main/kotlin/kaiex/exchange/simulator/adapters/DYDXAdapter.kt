package kaiex.exchange.simulator.adapters

import kaiex.model.OrderSide
import kaiex.model.Trade
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class DYDXAdapter(val symbol: String) : TickAdapter {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    override fun convert(line: String): Trade {
        val fields = line.split(",")
        val timestamp = LocalDateTime.parse(fields[0], formatter).atZone(ZoneId.systemDefault()).toInstant()
        val side = OrderSide.valueOf(fields[1])
        val size = fields[2].toBigDecimal()
        val price = fields[3].toBigDecimal()
        val isLiquidation = fields[4].toBoolean()

        return Trade(
            symbol = symbol,
            side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL,
            size = size,
            price = price,
            createdAt = timestamp,
            receivedAt = Instant.now(),
            liquidation = isLiquidation,
            historical = false
        )
    }
}