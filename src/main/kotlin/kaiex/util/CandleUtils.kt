import kaiex.model.Candle
import kaiex.model.Trade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Flow<Trade>.toCandles(): Flow<Candle> = flow {
    var open = 0f
    var high = 0f
    var low = 0f
    var close = 0f
    var time = Instant.now()
    var symbol = ""
    var tradeCount = 0

    collect { trade ->
        if (symbol != trade.symbol) {

            // Start a new candle and emit it
            symbol = trade.symbol
            open = trade.price
            high = trade.price
            low = trade.price
            close = trade.price
            time = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            emit(Candle(time.epochSecond, open, high, low, close, ++tradeCount, 0f))

        } else if (trade.createdAt.truncatedTo(ChronoUnit.MINUTES) != time) {

            // Emit the completed candle
            emit(Candle(time.epochSecond, open, high, low, close, ++tradeCount, 0f))

            // reset values for next candle
            open = trade.price
            high = trade.price
            low = trade.price
            close = trade.price
            time = trade.createdAt.truncatedTo(ChronoUnit.MINUTES)
            tradeCount = 0
            emit(Candle(time.epochSecond, open, high, low, close, ++tradeCount, 0f))

        } else {
            // Update the current candle with the latest trade
            high = maxOf(high, trade.price)
            low = minOf(low, trade.price)
            close = trade.price
            emit(Candle(time.epochSecond, open, high, low, close, ++tradeCount, 0f))
        }
        //delay(1) // Simulate processing time
    }
}