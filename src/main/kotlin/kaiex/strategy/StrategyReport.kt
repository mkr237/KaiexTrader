package kaiex.strategy

import kaiex.model.Candle
import kaiex.model.Order
import kaiex.model.Position
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class StrategyReport(var strategyId:String,
                          var pnl: Double = 0.0,
                          var orders: MutableList<Order> = mutableListOf(),
                          var positions: MutableList<Position> = mutableListOf(),
                          var candle: Candle? = null,
                          var marketData: MutableMap<String, Double> = mutableMapOf(),
                          var timeStamp: Long = Instant.now().epochSecond)