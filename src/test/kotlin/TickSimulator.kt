import kaiex.core.format
import kaiex.indicator.MACD
import kaiex.model.Candle
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kaiex.util.BinanceAdapter
import kaiex.util.TickPlayer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString as myJsonEncode

@Serializable
data class Update(val timestamp: Long, val candle:Candle, val macd:Double, val signal: Double, val histogram: Double)

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

        async {
            println("Reading from file...")
            TickPlayer("Binance BTCUSDT-trades-2023-04-04.csv", BinanceAdapter("BTCUSDT"), relativeTime = true).start()
                .toCandles().collect { candle ->

                    macd.update(candle.close.toDouble())
                    val macdLine = macd.getMACDLine()
                    val signalLine = macd.getSignalLine()
                    val histogram = macd.getHistogram()
                    val update = Update(candle.startTimestamp, candle, macdLine, signalLine, histogram)
                    println(update)
                    ui.sendData(route, DataPacket(0, format.myJsonEncode(update)))
            }
        }
    }
}