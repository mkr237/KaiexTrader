package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kaiex.model.*
import kaiex.util.Resource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class DYDXAccountSocketEndpoint: DYDXSocketEndpoint<AccountUpdate> {

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
                            val account: Account,
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

//    "fundingPayments": [
//    {
//        "accountId": "8d6c86b2-3dd5-5683-b720-00095db9658c",
//        "positionId": "9ea6049c-f1fd-5fa5-96cb-e3aae503cd04",
//        "transactionId": "2805295",
//        "market": "BTC-USD",
//        "payment": "0.194443",
//        "positionSize": "-0.46",
//        "price": "26847.63557277619838714599609375",
//        "rate": "0.0000157445794332",
//        "effectiveAt": "2023-05-21T12:00:00.000Z",
//        "id": "9051d272-ddf7-5fba-b17e-579eec0d3258",
//        "updatedAt": "2023-05-21T12:00:15.437Z",
//        "createdAt": "2023-05-21T12:00:15.437Z"
//    }
//    ]

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
        val exitPrice: String? = null,
        val openTransactionId: String,
        val closeTransactionId: String? = null,
        val lastTransactionId: String,
        val closedAt: String? = null,
        val updatedAt: String,
        val createdAt: String,
        val sumOpen: String,
        val sumClose: String,
        val netFunding: String,
        val unrealisedPnl: String? = null,
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
            json(Json { ignoreUnknownKeys = true })
        }
    }
    private var socket: WebSocketSession? = null

    override suspend fun connect(): Resource<Unit> {
        return try {
            log.info(DYDXSocketEndpoint.Endpoints.DYDXSocket.getURL())
            socket = client.webSocketSession {
                url(DYDXSocketEndpoint.Endpoints.DYDXSocket.getURL())
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

        val nowISO = getISOTime()
        val sig = sign("/ws/accounts",
            "GET",
            nowISO,
            mapOf())

        return try {
            val d = JsonObject(mapOf("type" to JsonPrimitive("subscribe"),
                                     "channel" to JsonPrimitive("v3_accounts"),
                                     "accountNumber" to JsonPrimitive("0"),     // TODO pass in!
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
                    when(val dydxAccountUpdate = Json.decodeFromString<Message>(json)) {
                        is Connected -> onConnected(dydxAccountUpdate)
                        is Subscribed -> emit(onSubscribed(dydxAccountUpdate))
                        is ChannelData -> emit(onChannelData(dydxAccountUpdate))
                        else -> {}
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

    private fun onSubscribed(message: Subscribed): AccountUpdate {
        log.debug("Subscribed with $message")
        return AccountUpdate(
            message.id,
            convertOrders(message.contents.orders),
            emptyList(),
            emptyList() // TODO convert open positions
        )
    }

    private fun onChannelData(message: ChannelData): AccountUpdate {
        log.info("Received channel data: $message")
        return AccountUpdate(
            message.id,
            convertOrders(message.contents.orders),
            convertFills(message.contents.fills),
            convertPositions(message.contents.positions))
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    private fun convertOrders(orders:List<Order>?) = orders?.map { order ->
            OrderUpdate(
                order.clientId,
                order.id,
                order.accountId,
                order.market,
                OrderType.valueOf(order.type),
                OrderSide.valueOf(order.side),
                order.price.toFloat(),
                order.size.toFloat(),
                order.remainingSize.toFloat(),
                OrderStatus.valueOf(order.status),
                OrderTimeInForce.valueOf(order.timeInForce),
                Instant.parse(order.createdAt).epochSecond,
                Instant.parse(order.expiresAt).epochSecond
            )
        }?: emptyList()

    private fun convertFills(fills:List<Fill>?) = fills?.map { fill ->
        OrderFill(
            fill.id,
            fill.orderClientId,
            fill.market,
            OrderType.valueOf(fill.type),
            OrderSide.valueOf(fill.side),
            fill.price.toFloat(),
            fill.size.toFloat(),
            fill.fee.toFloat(),
            OrderRole.valueOf(fill.liquidity),
            Instant.parse(fill.createdAt).epochSecond,
            Instant.parse(fill.updatedAt).epochSecond
        )
    }?: emptyList()

    private fun convertPositions(positions:List<Position>?) = positions?.map { position ->
        Position(
            position.id,
            position.market,
            PositionSide.valueOf(position.side),
            position.entryPrice.toFloat(),
            position.exitPrice?.toFloat() ?: 0f,
            position.size.toFloat(),
            position.unrealisedPnl?.toFloat() ?: 0f,
            Instant.parse(position.createdAt).epochSecond,
            position.closedAt?.let { Instant.parse(it).epochSecond } ?: 0
        )
    }?: emptyList()
}