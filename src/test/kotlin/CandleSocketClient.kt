import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.exchange.dydx.DYDXSocket
import kaiex.exchange.dydx.DYDXTradeStream
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

suspend fun main() {

    val URL = "ws://localhost:8081/data-feed"

    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30000
        }

        install(Logging) {
            level = LogLevel.INFO
        }
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }
    }

    var socket: WebSocketSession? = null

    try {
        socket = client.webSocketSession {
            url(URL)
        }
        if(socket?.isActive == true) {
            println("OPEN")

            while(true) {
                val othersMessage = socket.incoming.receive() as? Frame.Text
                println(othersMessage?.readText())
            }

        } else {
            println("FAILED")
        }
    } catch(e: Exception) {
        e.printStackTrace()
    }
}