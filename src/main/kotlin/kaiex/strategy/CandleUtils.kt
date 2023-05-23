import kaiex.model.Candle
import kaiex.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Flow<Trade>.toCandles(): Flow<Candle> = flow {

    var startTime:Instant? = null
    var lastUpdate:Instant? = null
    var symbol = ""
    var open = BigDecimal.ZERO
    var high = BigDecimal.ZERO
    var low = BigDecimal.ZERO
    var close = BigDecimal.ZERO
    var tradeCount = 0
    var totalVolume = BigDecimal.ZERO

    collect { trade ->
        if (startTime == null) {

            // create and send new candle
            symbol = trade.symbol
            open = trade.price
            high = trade.price
            low = trade.price
            close = trade.price
            tradeCount = 1
            totalVolume = trade.size
            startTime = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            lastUpdate = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(symbol, startTime!!, lastUpdate!!, open, high, low, close, tradeCount, totalVolume, trade.historical,false))

        } else if (trade.createdAt.truncatedTo(ChronoUnit.MINUTES) != startTime) {

            // send the previous candle
            emit(Candle(symbol, startTime!!, lastUpdate!!, open, high, low, close, tradeCount, totalVolume, trade.historical,true))

            // create and send new candle
            open = trade.price
            high = trade.price
            low = trade.price
            close = trade.price
            tradeCount = 1
            totalVolume = trade.size
            startTime = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            lastUpdate = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(symbol, startTime!!, lastUpdate!!, open, high, low, close, tradeCount, totalVolume, trade.historical,false))

        } else {

            // update value and send updated candle
            high = maxOf(high, trade.price)
            low = minOf(low, trade.price)
            close = trade.price
            tradeCount++
            totalVolume += trade.size
            lastUpdate = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(symbol, startTime!!, lastUpdate!!, open, high, low, close, tradeCount, totalVolume, trade.historical,false))
        }
    }
}