import kaiex.core.format
import kaiex.indicator.MACD
import kaiex.model.MACDUpdate
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kaiex.util.BinanceAdapter
import kaiex.util.TickPlayer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString as myJsonEncode

suspend fun main() {

    val route = "/Simulator"
    val ui = UIServer()

    val macd = MACD(12, 26, 9)

    coroutineScope {
        async {
            println("Starting UI server...")
            ui.start()
        }

        delay(2000)
        ui.createSocket(route)

        // provide a chance to connect the browser
        delay(5000)

        async {
            println("Reading from file...")
            var lastCandle: Long? = null
            TickPlayer("Binance BTCUSDT-trades-2023-04-04.csv", BinanceAdapter("BTCUSDT"), relativeTime = false).start()
                .toCandles().collect { candle ->

                    if(candle.startTimestamp != lastCandle) {
                        macd.update(candle.close.toDouble())
                        val macdLine = macd.getMACDLine()
                        val signalLine = macd.getSignalLine()
                        val histogram = macd.getHistogram()
                        val update = MACDUpdate(candle.startTimestamp, macdLine, signalLine, histogram)
                        println(update)
                        ui.sendData(route, DataPacket(0, format.myJsonEncode(update)))
                        lastCandle = candle.startTimestamp
                    }

                    println(candle)
                    ui.sendData(route, DataPacket(0, format.myJsonEncode(candle)))

                    delay(1)
            }
        }
    }
}