package kaiex.ui

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Serializable
data class DataPacket(val index: Int, val data: String)

private const val updateTimeout = 1000L
private const val heartbeatPeriod = 5000L

class UIServer : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var server:NettyApplicationEngine? = null

    private var inputQueues: MutableMap<String, Channel<DataPacket>> = mutableMapOf()

    fun start() {
        log.info("Starting...")
        server = embeddedServer(Netty, 8081) {
            install(WebSockets)
            install(CORS) {// TODO - Need this?
                anyHost()
            }
        }

        server!!.start(wait = true)
    }

    fun createSocket(routeId: String, requestHistory: () -> Unit) {

        // TMp
        if(inputQueues.containsKey(routeId))
            throw RuntimeException("Channel exists")

        inputQueues[routeId] = Channel(capacity = 100)

        log.info("Creating web socket with route: $routeId")
        val app = server?.application

        app?.routing {
            webSocket(routeId) {

                log.info("Received connection for route: $routeId")

                //requestHistory()

                // set up heartbeat job
                val heartbeatJob = launch {
                    while (isActive) {
                        log.debug("Sending heartbeat to client for route $routeId")
                        outgoing.send(Frame.Text("heartbeat"))
                        delay(heartbeatPeriod)
                    }
                }

                // send outgoing messages
                try {

                    launch {
                        withTimeoutOrNull(heartbeatPeriod) {
                        incoming.receive() } ?: throw ClosedReceiveChannelException("Heartbeat failure")
                    }

                    while (isActive) {
                        val packet = inputQueues[routeId]?.receive()
                        log.debug("Sending: $packet")
                        outgoing.send(Frame.Text(packet!!.data))
                    }

                } catch (e: Exception) {
                    if (e is ClosedReceiveChannelException) {
                        log.info("Client disconnected for route $routeId")
                        outgoing.close()
                    } else {
                        log.error("Socket connection failed for route $routeId")
                    }

                } finally {
                    heartbeatJob.cancel()
                }
            }
        }
    }

    fun sendData(routeId: String, packet: DataPacket) {
        log.debug("Received: $packet")
        if(inputQueues.containsKey(routeId))
            inputQueues[routeId]?.trySend(packet)
    }
}
