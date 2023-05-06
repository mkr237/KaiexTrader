package kaiex.exchange.dydx

import com.fersoft.signature.StarkSigner
import com.fersoft.types.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kaiex.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger


class DYDXOrderEndpoint: DYDXHttpEndpoint<OrderUpdate> {

    @Serializable
    data class OrderRequest(
        val market: String,
        val side: String,
        val type: String,
        val timeInForce: String,
        val size: String,
        val price: String,
        val limitFee: String,
        val expiration: String,
        val postOnly: Boolean,
        val clientId: String,
        val signature: String,
        val reduceOnly: String
    )

    @Serializable
    data class OrderResponse(
        val order: Order
    )

    @Serializable
    data class Order(
        val id: String,
        val clientId: String,
        val accountId: String,
        val market: String,
        val side: String,
        val price: String,
        val triggerPrice: String?,
        val trailingPercent: String?,
        val size: String,
        val reduceOnlySize: String?,
        val remainingSize: String,
        val type: String,
        val createdAt: String,
        val unfillableAt: String?,
        val expiresAt: String,
        val status: String,
        val timeInForce: String,
        val postOnly: Boolean,
        val reduceOnly: Boolean,
        val cancelReason: String?
    )

    val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    val customJson = Json {
        classDiscriminator = "message-type"
    }

    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30000
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun get(): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun post(order: CreateOrder): Result<String> {

        log.info("Sending order: $order")

        return try {
            //
            val request = createOrderToOrderRequest(order)

            val nowISO = getISOTime()
            val orderAsMap = orderRequestToMap(request)
            val apiSignature = sign(DYDXHttpEndpoint.Endpoints.DYDXOrders.ext, "POST", nowISO, orderAsMap)

            val responseOrder: HttpResponse = client.post(DYDXHttpEndpoint.Endpoints.DYDXOrders.getURL()) {
                contentType(ContentType.Application.Json)
                setBody(orderAsMap)
                headers {
                    append("DYDX-API-KEY", DYDX_API_KEY)
                    append("DYDX-SIGNATURE", apiSignature)
                    append("DYDX-PASSPHRASE", DYDX_API_PASSPHRASE)
                    append("DYDX-TIMESTAMP", nowISO)
                }
            }

            // print response
            val orderResponse = Json.decodeFromString<OrderResponse>(responseOrder.body())
            log.info("Got order response $orderResponse")

            val response = orderResponseToOrderUpdate(orderResponse)
            Result.success(response.orderId)

        } catch(e: Exception) {
            Result.failure(e)
        }
    }

    // TODO these functions are all flimsy and we need to find a better way to convert to/from each API
    private fun createOrderToOrderRequest(createOrder: CreateOrder): OrderRequest {

        val positionId = "5630"  // TODO get from account
        val expiration = getISOTime(plusMins = 60)

        // create Stark signature for the order
        val starkPrivateKey = System.getenv("STARK_PRIVATE_KEY")
        val startkPrivateKeyInt = BigInteger(starkPrivateKey, 16)
        val signableOrder = Order(
            positionId,
            createOrder.size.toString(),
            createOrder.limitFee.toString(),
            DydxMarket.fromString(createOrder.symbol),
            StarkwareOrderSide.valueOf(createOrder.side.toString()),
            expiration)

        val orderWithPrice = OrderWithClientIdWithPrice(signableOrder, createOrder.orderId, createOrder.price.toString())
        val starkSignature = StarkSigner().sign(orderWithPrice, NetworkId.GOERLI, startkPrivateKeyInt)

        // return the converted order
        return OrderRequest(
            market = createOrder.symbol,
            side = createOrder.side.toString(),
            type = createOrder.type.toString(),
            timeInForce = createOrder.timeInForce.toString(),
            size = createOrder.size.toString(),
            price = createOrder.price.toString(),
            limitFee = createOrder.limitFee.toString(),
            expiration = expiration,
            postOnly = createOrder.postOnly,
            clientId = createOrder.orderId,
            signature = starkSignature.toString(),
            reduceOnly = createOrder.reduceOnly.toString()
        )
    }

    private fun orderRequestToMap(orderRequest: OrderRequest): Map<String, String> {
        return mapOf(
            "market" to orderRequest.market,
            "side" to orderRequest.side,
            "type" to orderRequest.type,
            "timeInForce" to orderRequest.timeInForce,
            "size" to orderRequest.size,
            "price" to orderRequest.price,
            "limitFee" to orderRequest.limitFee,
            "expiration" to orderRequest.expiration,
            "postOnly" to orderRequest.postOnly.toString(),
            "clientId" to orderRequest.clientId,
            "signature" to orderRequest.signature,
            "reduceOnly" to orderRequest.reduceOnly
        )
    }

    private fun orderResponseToOrderUpdate(orderResponse: OrderResponse): OrderUpdate {
        return OrderUpdate(
            orderId = orderResponse.order.clientId,
            exchangeId = orderResponse.order.id,
            accountId = orderResponse.order.accountId,
            symbol = orderResponse.order.market,
            type = OrderType.valueOf(orderResponse.order.type),
            side = OrderSide.valueOf(orderResponse.order.side),
            price = orderResponse.order.price.toFloat(),
            size = orderResponse.order.size.toFloat(),
            remainingSize = orderResponse.order.remainingSize.toFloat(),
            status = OrderStatus.valueOf(orderResponse.order.status),
            timeInForce = OrderTimeInForce.valueOf(orderResponse.order.timeInForce),
            createdAt = isoTimeStringToMillis(orderResponse.order.createdAt),
            expiresAt = isoTimeStringToMillis(orderResponse.order.expiresAt)
        )
    }
}