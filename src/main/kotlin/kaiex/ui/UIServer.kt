package kaiex.ui

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kaiex.core.format
import kaiex.strategy.StrategyChartConfig
import kaiex.strategy.StrategyMarketDataUpdate
import kaiex.strategy.StrategySnapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString as myJsonEncode

private const val CHANNEL_SIZE = 1000  // TODO too big?
private const val HISTORY_SIZE = CHANNEL_SIZE
private const val SERVER_PORT = 8081

class UIServer : KoinComponent {

    private val simpleName = javaClass.simpleName
    private val log: Logger = LoggerFactory.getLogger(simpleName)

    private var inputQueue: Channel<Message> = Channel(capacity = CHANNEL_SIZE)
    private val messageHistory = LinkedList<Message>()
    private var sessionNumber = 0
    private var sequenceNumber = 0L

    private var strategies = mutableMapOf<String, StrategyChartConfig>()

    fun start() {
        log.info("Starting...")

        val server = embeddedServer(Netty, port = SERVER_PORT) {
            install(WebSockets)

            val sessions = ConcurrentHashMap<String, WebSocketSession>()

            routing {
                webSocket("/strategy") {

                    val sessionId = "Session-${++sessionNumber}"
                    val sessionLog: Logger = LoggerFactory.getLogger("$simpleName:$sessionId")
                    sessionLog.info("Creating session: $sessionId")
                    sessions[sessionId] = this

                    try {

                        // send strategy descriptors to client
                        sessionLog.info("Sending ${strategies.size} strategy descriptors to $sessionId")
                        strategies.values.forEach { config ->
                            val message = StrategyDescriptorMessage(++sequenceNumber, "kaiex.ui.StrategyChartConfigMessage", config)
                            send(Frame.Text(format.myJsonEncode(message)))
                        }

                        // Send message history to new session
                        sessionLog.info("Sending history of ${messageHistory.size} to $sessionId")
                        messageHistory.forEach { message ->
                            sessionLog.info("Sending $message to $sessionId")
                            send(Frame.Text(format.myJsonEncode(message)))
                        }

                        while (true) {
                            when (val frame = incoming.receive()) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    sessionLog.info("Received message from $sessionId: $text")
                                    sessions.values.forEach { session ->
                                        if (session != this) {
                                            session.send(text)
                                        }
                                    }
                                }
                                is Frame.Close -> {
                                    sessionLog.info("Socket Closed")
                                }
                                else -> {
                                    sessionLog.error("Received unknown message")
                                }
                            }
                        }
                    } catch (e: ClosedReceiveChannelException) {
                        sessionLog.error("Session $sessionId closed: ${e.message}")
                    } catch (e: Throwable) {
                        sessionLog.error("Error occurred in Session $sessionId: ${e.message}")
                    } finally {
                        sessions.remove(sessionId)
                        sessionLog.info("Session $sessionId closed")
                    }
                }
            }

            launch {
                while (true) {
                    val message = inputQueue.receive()

                    messageHistory.add(message!!)
                    if (messageHistory.size > HISTORY_SIZE) {
                        messageHistory.removeFirst()
                    }

                    sessions.forEach { (key, session) ->
                        try {
                            session.send(Frame.Text(format.myJsonEncode(message)))
                        } catch (e: Throwable) {
                            log.info("Error occurred while sending message to $key: ${e.message}")
                            session.close(CloseReason(CloseReason.Codes.UNEXPECTED_CONDITION, "Failed to send message"))
                        }
                    }
                }
            }
        }

        server.start(wait = true)
    }

    fun register(config: StrategyChartConfig) {
        log.debug("Registering strategy: $config")
        strategies[config.strategyId] = config
    }

    fun send(update: StrategySnapshot) {
        log.debug("Received: $update")
        inputQueue.trySend(StrategySnapshotMessage(++sequenceNumber, update))
    }

    fun send(update: StrategyMarketDataUpdate) {
        log.debug("Received: $update")
        inputQueue.trySend(StrategyMarketDataUpdateMessage(++sequenceNumber, update))
    }
}