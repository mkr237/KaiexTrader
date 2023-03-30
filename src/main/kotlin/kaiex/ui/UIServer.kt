package kaiex.ui

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Serializable
data class DataPacket(val lastIndex: Int, val data: String)

private const val updateTimeout = 1000L
private const val heartbeatPeriod = 5000L

class UIServer : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var server:NettyApplicationEngine? = null

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

    fun addDataStream(routeId: String, getDataFromIndex: (Int) -> DataPacket?) {

        log.info("Creating web socket with route: $routeId")
        val app = server?.application

        app?.routing {
            webSocket(routeId) {

                log.info("Received connection for route: $routeId")

                // Set up heartbeat job
                val heartbeatJob = launch {
                    while (isActive) {
                        log.debug("Sending heartbeat to client for route $routeId")
                        outgoing.send(Frame.Text("heartbeat"))
                        delay(heartbeatPeriod)
                    }
                }

                // try to send outgoing messages
                try {
                    var nextIndex = 0
                    val initialPacket = getDataFromIndex(0)
                    if (initialPacket != null) nextIndex = sendData(outgoing, initialPacket)
                    delay(updateTimeout)

                    while (true) {
                        val packet = getDataFromIndex(nextIndex)
                        if (packet != null) nextIndex = sendData(outgoing, packet)
                        delay(updateTimeout)

                        withTimeoutOrNull(heartbeatPeriod) {
                            incoming.receive()
                        }?: throw ClosedReceiveChannelException("Heartbeat failure")
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

     private suspend fun sendData(channel:SendChannel<Frame>, packet:DataPacket):Int {
        log.debug("Received data: $packet")
        channel.send(Frame.Text(packet.data))
        return packet.lastIndex + 1
    }
}
