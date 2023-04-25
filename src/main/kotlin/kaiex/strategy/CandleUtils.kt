import kaiex.model.Candle
import kaiex.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Flow<Trade>.toCandles(): Flow<Candle> = flow {

    var startTime:Instant? = null
    var lastUpdate:Instant? = null
    var symbol = ""
    var open = 0f
    var high = 0f
    var low = 0f
    var close = 0f
    var tradeCount = 0
    var totalVolume = 0f

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
            emit(Candle(symbol, startTime!!.epochSecond, lastUpdate!!.epochSecond, open, high, low, close, tradeCount, totalVolume, trade.historical))

        } else if (trade.createdAt.truncatedTo(ChronoUnit.MINUTES) != startTime) {

            // send the previous candle
            emit(Candle(symbol, startTime!!.epochSecond, lastUpdate!!.epochSecond, open, high, low, close, tradeCount, totalVolume, trade.historical))

            // create and send new candle
            open = trade.price
            high = trade.price
            low = trade.price
            close = trade.price
            tradeCount = 1
            totalVolume = trade.size
            startTime = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            lastUpdate = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(symbol, startTime!!.epochSecond, lastUpdate!!.epochSecond, open, high, low, close, tradeCount, totalVolume, trade.historical))

        } else {

            // update value and send updated candle
            high = maxOf(high, trade.price)
            low = minOf(low, trade.price)
            close = trade.price
            tradeCount++
            totalVolume += trade.size
            lastUpdate = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(symbol, startTime!!.epochSecond, lastUpdate!!.epochSecond, open, high, low, close, tradeCount, totalVolume, trade.historical))
        }
    }
}