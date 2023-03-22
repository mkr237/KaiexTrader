package kaiex.services.dydx

import com.kaiex.services.dydx.DYDXSocket
import com.kaiex.util.Resource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.AccountUpdate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DYDXAccountStream(): DYDXSocket<AccountUpdate> {

    //private val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    private val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    private val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    //private val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30000
        }
        install(Logging)
        install(WebSockets)
        install(ContentNegotiation) {
            json()
        }
    }
    private var socket: WebSocketSession? = null

    override suspend fun connect(): Resource<Unit> {
        return try {
            socket = client.webSocketSession {
                url(DYDXSocket.Endpoints.DYDXSocket.url)
            }
            if(socket?.isActive == true) {
                Resource.Success(Unit)
            } else Resource.Error("Couldn't establish a connection.")
        } catch(e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to connect")
        }
    }

    override suspend fun subscribe(): Resource<Unit> {

        val nowISO = nowISO()
        val sig = sign("/ws/accounts",
            "GET",
            nowISO,
            mapOf())

        return try {
            val d = JsonObject(mapOf("type" to JsonPrimitive("subscribe"),
                                     "channel" to JsonPrimitive("v3_accounts"),
                                     "accountNumber" to JsonPrimitive("0"),
                                     "apiKey" to JsonPrimitive(DYDX_API_KEY),
                                     "signature" to JsonPrimitive(sig),
                                     "timestamp" to JsonPrimitive(nowISO),
                                     "passphrase" to JsonPrimitive(DYDX_API_PASSPHRASE)
            ))
            socket?.send(Frame.Text(d.toString()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.localizedMessage ?: "Failed to subscribe")
        }
    }

    override fun observeUpdates(): Flow<AccountUpdate> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.transform {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    log.info(json)
//                    when(val dydxAccountUpdate = Json.decodeFromString<DYDXTradeStream.Message>(json)) {
//                        is DYDXTradeStream.Connected -> onConnected(dydxAccountUpdate)
//                        is DYDXTradeStream.Subscribed -> onSubscribed(dydxAccountUpdate).forEach{ trade -> emit(trade) }
//                        is DYDXTradeStream.ChannelData -> onChannelData(dydxAccountUpdate).forEach{ trade -> emit(trade) }
//                    }
                }
                ?: flow { }
        } catch(e: Exception) {
            e.printStackTrace()
            flow { }
        }
    }
//
//    private fun onConnected(message: DYDXTradeStream.Connected) {
//        log.info("Connected with connection id: ${message.connection_id}")
//    }
//
//    private fun onSubscribed(message: DYDXTradeStream.Subscribed): List<com.kaiex.model.Trade> {
//        log.debug("Subscribed to $message")
//        return convertTradeList(message.contents.trades)
//    }
//
//    private fun onChannelData(message: DYDXTradeStream.ChannelData): List<com.kaiex.model.Trade> {
//        log.debug("Received channel data for $message")
//        return convertTradeList(message.contents.trades)
//    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }
}