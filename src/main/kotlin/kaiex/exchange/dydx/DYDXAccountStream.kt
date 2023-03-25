package kaiex.exchange.dydx

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.AccountInfo
import kaiex.model.AccountSnapshot
import kaiex.model.AccountUpdate
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DYDXAccountStream(): DYDXSocket<AccountInfo> {

    @Serializable(with = MessageSerializer::class)
    sealed class Message {
        abstract val type: String
        abstract val connection_id: String
        abstract val message_id: Int
    }

    @Serializable
    data class Connected(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int
    ) : Message()

    @Serializable
    data class Subscribed(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int,
        val channel: String,
        val id: String,
        val contents: Contents
    ) : Message() {
        @Serializable
        data class Contents(val orders: List<Order>? = emptyList(),
                            val account:Account,
                            val transfers: List<Transfer>? = emptyList(),
                            val fundingPayments: List<FundingPayment>? = emptyList())
    }

    @Serializable
    data class ChannelData(
        override val type: String,
        override val connection_id: String,
        override val message_id: Int,
        val id: String,
        val channel: String,
        val contents: Contents
    ) : Message() {
        @Serializable
        data class Contents(val orders: List<Order>? = emptyList(),
                            val fills: List<Fill>? = emptyList(),
                            val positions: List<Position>? = emptyList(),
                            val accounts: List<AccountBalance>? = emptyList(),
                            val transfers: List<Transfer>? = emptyList(),
                            val fundingPayments: List<FundingPayment>? = emptyList())
    }

    @Serializable
    data class Account(
        val starkKey: String,
        val positionId: String,
        val equity: String,
        val freeCollateral: String,
        val pendingDeposits: String,
        val pendingWithdrawals: String,
        val openPositions: Map<String, OpenPosition>? = emptyMap(),
        val accountNumber: String,
        val id: String,
        val quoteBalance: String,
        val createdAt: String
    )

    @Serializable
    data class OpenPosition(
        val market: String,
        val status: String,
        val side: String,
        val size: String,
        val maxSize: String,
        val entryPrice: String,
        val exitPrice: String? = null,
        val unrealizedPnl: String,
        val realizedPnl: String,
        val createdAt: String,
        val closedAt: String? = null,
        val sumOpen: String,
        val sumClose: String,
        val netFunding: String
    )

    @Serializable
    data class Transfer(
        val id: String,
        val type: String,
        val debitAsset: String,
        val creditAsset: String,
        val debitAmount: String,
        val creditAmount: String,
        val transactionHash: String? = null,
        val status: String,
        val createdAt: String,
        val confirmedAt: String,
        val clientId: String,
        val fromAddress: String,
        val toAddress: String,
        val accountId: String,
        val transferAccountId: String
    )

    @Serializable
    data class FundingPayment(
        val market: String,
        val payment: String,
        val rate: String,
        val positionSize: String,
        val price: String,
        val effectiveAt: String
    )

    @Serializable
    data class Order(
        val id: String,
        val status: String,
        val accountId: String,
        val clientId: String,
        val market: String,
        val side: String,
        val price: String,
        val limitFee: String? = null,
        val size: String,
        val remainingSize: String,
        val type: String,
        val signature: String? = null,
        val expiresAt: String,
        val timeInForce: String,
        val postOnly: Boolean,
        val reduceOnly: Boolean,
        val country: String? = null,
        val client: String? = null,
        val ipAddress: String? = null,
        val reduceOnlySize: String? = null,
        val triggerPrice: String? = null,
        val trailingPercent: String? = null,
        val unfillableAt: String? = null,
        val cancelReason: String? = null,
        val updatedAt: String? = null,
        val createdAt: String
    )

    @Serializable
    data class Fill(
        val market: String,
        val transactionId: String,
        val quoteAmount: String,
        val price: String,
        val size: String,
        val liquidity: String,
        val accountId: String,
        val side: String,
        val orderId: String,
        val fee: String,
        val type: String,
        val id: String,
        val nonce: String? = null,
        val forcePositionId: String? = null,
        val updatedAt: String,
        val createdAt: String,
        val orderClientId: String
    )

    @Serializable
    data class Position(
        val id: String,
        val accountId: String,
        val market: String,
        val side: String,
        val status: String,
        val size: String,
        val maxSize: String,
        val entryPrice: String,
        val exitPrice: String?,
        val openTransactionId: String,
        val closeTransactionId: String? = null,
        val lastTransactionId: String,
        val closedAt: String? = null,
        val updatedAt: String,
        val createdAt: String,
        val sumOpen: String,
        val sumClose: String,
        val netFunding: String,
        val realizedPnl: String
    )

    @Serializable
    data class AccountBalance(
        val id: String,
        val userId: String,
        val accountNumber: Int,
        val starkKey: String,
        val quoteBalance: String,
        val lastTransactionId: String,
        val updatedAt: String,
        val createdAt: String,
        val positionId: String,
        val starkKeyYCoordinate: String
    )

    object MessageSerializer :
        JsonContentPolymorphicSerializer<Message>(
            Message::class
        ) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Message> {
            return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
                "connected" -> Connected.serializer()
                "subscribed" -> Subscribed.serializer()
                "channel_data" -> ChannelData.serializer()
                else -> throw Exception("ERROR: No Serializer found. Serialization failed.")
            }
        }
    }

    //private val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    private val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    private val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    //private val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val client = HttpClient(CIO) {
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
    private var socket: WebSocketSession? = null

    // for pretty printing JSON // TODO move into parent class
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val jp = JsonParser()

    override suspend fun connect(): Resource
    <Unit> {
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

    override fun observeUpdates(): Flow<AccountInfo> {
        return try {
            socket?.incoming
                ?.receiveAsFlow()
                ?.filter { it is Frame.Text }
                ?.transform {
                    val json = (it as? Frame.Text)?.readText() ?: ""
                    log.debug("Received: " + gson.toJson(jp.parse(json)))
                    when(val dydxAccountUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxAccountUpdate)
                        is Subscribed -> onSubscribed(dydxAccountUpdate)
                        is ChannelData -> onChannelData(dydxAccountUpdate)
                    }
                }
                ?: flow { }
        } catch(e: Exception) {
            e.printStackTrace()
            flow { }
        }
    }

    private fun onConnected(message: Connected) {
        log.info("Connected with connection_id: ${message.connection_id}")
    }

    private fun onSubscribed(message: Subscribed): AccountSnapshot {
        log.info("Subscribed with id ${message.id}")
        return AccountSnapshot(message.contents.account.id)
    }

    private fun onChannelData(message: ChannelData): AccountUpdate {
        log.info("Received channel data")
        return AccountUpdate(message.id)
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }
}