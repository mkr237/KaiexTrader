import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString as myJsonEncode
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun main() {
    embeddedServer(Netty, port = 8081) {
        install(WebSockets)
        install(CORS) {
            anyHost()
        }
        routing {
            webSocket("/data-feed") {

                println("Client connected")
                try {
                    while (true) {

                        delay(1000L) // Wait for 1 second
                        val csd = generateCandlestick()
                        println("Sending: $csd")
                        outgoing.send(Frame.Text(Json.myJsonEncode(csd)))
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("Client disconnected")
                }

//                val channel = Channel<CandlestickData>() {}
//
//                // Launch a coroutine to generate candlestick data
//                launch { generateCandlestickStream(channel) }
//
//                try {
//                    // Continuously send candlestick data to the client
//                    for (candlestick in channel) {
//                        val candlestickJson = """
//                        {
//                          "timestamp": ${candlestick.timestamp},
//                          "open": ${candlestick.open},
//                          "high": ${candlestick.high},
//                          "low": ${candlestick.low},
//                          "close": ${candlestick.close}
//                        }
//                    """.trimIndent()
//
//                        send(Frame.Text(candlestickJson))
//                    }
//                } catch (e: Exception) {
//                    // Do nothing
//                } finally {
//                    channel.close()
//                }
            }
        }
    }.start(wait = true)
}

fun generateCandlestick(): CandlestickData {
    val now = LocalDateTime.now()
    val randomPrice = Random.nextDouble(100.0, 200.0)
    return CandlestickData(
        time = now.toEpochSecond(ZoneOffset.UTC),
        open = randomPrice,
        high = randomPrice + Random.nextDouble(0.0, 5.0),
        low = randomPrice - Random.nextDouble(0.0, 5.0),
        close = randomPrice + Random.nextDouble(-5.0, 5.0)
    )
}

fun generateCandlestickStream(channel: SendChannel<CandlestickData>) {
    runBlocking {
        while (true) {
            delay(1000L) // Wait for 1 second
            val csd = generateCandlestick()
            println("Sending: ${csd.toString()}")
            channel.send(csd)
        }
    }
}

@Serializable
data class CandlestickData(val time: Long, val open: Double, val high: Double, val low: Double, val close: Double)

//fun Application.module() {
//    install(WebSockets)
//    install(CORS) {
////        allowMethod(HttpMethod.Options)
////        allowMethod(HttpMethod.Put)
////        allowMethod(HttpMethod.Patch)
////        allowMethod(HttpMethod.Delete)
////        allowHeader(HttpHeaders.AccessControlAllowOrigin)
//        anyHost()
//    }
//    routing {
//        webSocket("/data-feed") {
//            val channel = Channel<CandlestickData>()
//
//            // Launch a coroutine to generate candlestick data
//            launch { generateCandlestickStream(channel) }
//
//            try {
//                // Continuously send candlestick data to the client
//                for (candlestick in channel) {
//                    val candlestickJson = """
//                        {
//                          "timestamp": ${candlestick.timestamp},
//                          "open": ${candlestick.open},
//                          "high": ${candlestick.high},
//                          "low": ${candlestick.low},
//                          "close": ${candlestick.close}
//                        }
//                    """.trimIndent()
//
//                    send(Frame.Text(candlestickJson))
//                }
//            } catch (e: Exception) {
//                // Do nothing
//            } finally {
//                channel.close()
//            }
//        }
//    }
//}
