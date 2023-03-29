package kaiex.util

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.TimeUnit

object WebSocketServer {
    private var server: NettyApplicationEngine? = null

    fun startServer() {
        server = embeddedServer(Netty, port = 8081) {
            install(io.ktor.server.websocket.WebSockets)
            install(CORS) {
                anyHost()
            }
            routing {
                webSocket("/data-feed") {
                    println("*** Client s connected")
                    WebSocketServerHandler.handleWebSocketConnection(this)
                    // handle incoming WebSocket connections
//                    for (frame in incoming) {
//                        if (frame is Frame.Text) {
//                            val message = frame.readText()
//                            send("You said: $message")
//                        }
//                    }
                }
            }
        }

        server?.start(wait = false)
    }

    fun stopServer() {
        server?.stop(0, 0, TimeUnit.MILLISECONDS)
    }

    suspend fun sendDataToSocket(message: String) {
        // get the WebSocketSession instances of all connected clients
        val sessions = WebSocketServerHandler.sessions.toList()

        // send data to all connected clients
        for (session in sessions) {
            session.outgoing.send(Frame.Text(message))
        }
    }

    fun isConnected():Boolean {
        return WebSocketServerHandler.sessions.isNotEmpty()
    }
}

object WebSocketServerHandler {
    val sessions = mutableListOf<WebSocketSession>()

    suspend fun handleWebSocketConnection(webSocketSession: WebSocketSession) {
        // add the WebSocketSession to the list of connected sessions
        sessions.add(webSocketSession)

        // handle incoming WebSocket connections
        for (frame in webSocketSession.incoming) {
            if (frame is Frame.Text) {
                val message = frame.readText()
                webSocketSession.send("You said: $message")
            }
        }

        // remove the WebSocketSession from the list of connected sessions
        sessions.remove(webSocketSession)
    }
}