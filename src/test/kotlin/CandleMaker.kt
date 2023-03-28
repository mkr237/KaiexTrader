
data class Trade(val timestamp: Long, val price: Double, val volume: Double)

fun main() {
    // Assume that we have a stream of trade events
    val tradeEvents = listOf(
        Trade(1616700000L, 100.0, 10.0),
        Trade(1616700001L, 102.0, 5.0),
        Trade(1616700002L, 101.5, 7.0),
        Trade(1616700004L, 103.0, 15.0),
        Trade(1616700005L, 101.0, 3.0),
        Trade(1616700006L, 99.0, 20.0),
        Trade(1616700007L, 98.0, 8.0),
        Trade(1616700008L, 97.0, 10.0),
        Trade(1616700009L, 99.5, 12.0),
        Trade(1616700010L, 100.5, 5.0)
    )

    // Group the trade events by minute
    val minuteGroups = tradeEvents.groupBy { it.timestamp / 60 }

    // Convert each group of trades into a single 1 minute candle
    val minuteCandles = minuteGroups.map { (minute, trades) ->
        val openPrice = trades.first().price
        val closePrice = trades.last().price
        val highPrice = trades.maxByOrNull { it.price }!!.price
        val lowPrice = trades.minByOrNull { it.price }!!.price
        val volume = trades.sumByDouble { it.volume }

        Trade(minute * 60, closePrice, volume)
    }

    // Print the 1 minute candles
    minuteCandles.forEach { println(it) }
}
