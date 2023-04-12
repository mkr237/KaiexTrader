package kaiex.ui

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
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
import java.util.LinkedList
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
data class DataPacket(val index: Int, val data: String)

private const val HEARTBEAT_PERIOD = 5000L
private const val CHANNEL_SIZE = 1000  // TODO too big?
private const val HISTORY_SIZE = CHANNEL_SIZE
private const val SERVER_PORT = 8081

class UIServer : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var server:NettyApplicationEngine? = null

    private var inputQueues: MutableMap<String, Channel<DataPacket>> = mutableMapOf()
    private val sessions = CopyOnWriteArrayList<WebSocketSession>()

    // TODO - needs to be per route
    private val messageHistory = LinkedList<DataPacket>()

    fun start() {
        log.info("Starting...")
        server = embeddedServer(Netty, SERVER_PORT) {
            install(WebSockets)
        }

        server!!.start(wait = true)
    }

    fun createSocket(routeId: String) {

        //
        if(inputQueues.containsKey(routeId))
            throw RuntimeException("Channel exists")  // TODO do something better

        inputQueues[routeId] = Channel(capacity = CHANNEL_SIZE)

        log.info("Creating web socket with route: $routeId")
        val app = server?.application

        app?.routing {
            webSocket(routeId) {

                log.info("Received connection for route: $routeId")

                // store session
                sessions += this

                // Send message history to new session
                log.info("Sending history of ${messageHistory.size}")
                messageHistory.forEach { packet ->
                    log.info("Sending $packet")
                    send(Frame.Text(packet.data))
                }

                // set up heartbeat job
                val heartbeatJob = launch {
                    while (isActive) {
                        log.debug("Sending heartbeat to client for route $routeId")
                        outgoing.send(Frame.Text("heartbeat"))
                        delay(HEARTBEAT_PERIOD)
                    }
                }

                // send outgoing messages
                try {

                    launch {
                        withTimeoutOrNull(HEARTBEAT_PERIOD) {
                        incoming.receive() } ?: throw ClosedReceiveChannelException("Heartbeat failure")
                    }

                    while (isActive) {
                        val packet = inputQueues[routeId]?.receive()

                        //outgoing.send(Frame.Text(packet!!.data))

                        messageHistory.add(packet!!)
                        if (messageHistory.size > HISTORY_SIZE) {
                            messageHistory.removeFirst()
                        }

                            //log.info("Sending to ${sessions.size} sessions: $packet")
                        sessions.forEach {
                            it.send(Frame.Text(packet.data))
                        }
                    }

                } catch (e: Exception) {
                    if (e is ClosedReceiveChannelException) {
                        log.info("Client disconnected for route $routeId")
                        outgoing.close()
                    } else {
                        log.error("Socket connection failed for route $routeId: $e")
                    }

                } finally {
                    sessions -= this
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
