import kaiex.indicator.MACD
import kaiex.exchange.simulator.TickPlayer
import kaiex.exchange.simulator.adapters.BinanceAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

suspend fun main() {

    val route = "/Simulator"

    val macd = MACD(12, 26, 9)

    coroutineScope {
//        async {
//            println("Starting UI server...")
//            ui.start()
//        }

        delay(2000)
        //ui.createSocket(route)

        // provide a chance to connect the browser
        delay(5000)

        async {
            println("Reading from file...")
            var lastCandle: Instant? = null
            TickPlayer("Binance BTCUSDT-trades-2023-04-04.csv", BinanceAdapter("BTCUSDT"), relativeTime = false).start()
                .toCandles(ChronoUnit.MINUTES).collect { candle ->

                    if(candle.startTimestamp != lastCandle) {
                        macd.update(candle.close.toDouble())
                        val macdLine = macd.getMACDLine()
                        val signalLine = macd.getSignalLine()
                        val histogram = macd.getHistogram()
                        //ui.sendMessage("macd", format.myJsonEncode(update))
                        lastCandle = candle.startTimestamp
                    }

                    println(candle)
                    //ui.sendMessage("candle", format.myJsonEncode(candle))

                    delay(1)
            }
        }
    }
}