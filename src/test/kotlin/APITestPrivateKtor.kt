import com.fersoft.converters.StarkwareOrderConverter
import com.fersoft.hashing.ConstantPoints
import com.fersoft.hashing.PedersonHash
import com.fersoft.hashing.StarkHashCalculator
import com.fersoft.signature.EcSigner
import com.fersoft.signature.StarkCurve
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
import kaiex.exchange.dydx.*
import kaiex.model.OrderSide
import kaiex.model.OrderType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.util.UUID


suspend fun main() {

    val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    val baseURL = "https://api.stage.dydx.exchange"
    val accountURL = "/v3/accounts/${DYDX_ACCOUNT_ID}"
    val ordersURL = "/v3/orders"

    val client = HttpClient(CIO) {
        engine {
            requestTimeout = 30000
        }
        install(Logging)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }

    try {

        /**
         * GET ACCOUNT TEST
         */
        val nowISO = nowISO()
        val sig = sign(accountURL, "GET", nowISO, mapOf())

        val response: HttpResponse = client.get(baseURL + accountURL) {
            headers {
                append("DYDX-API-KEY", DYDX_API_KEY)
                append("DYDX-SIGNATURE", sig)
                append("DYDX-PASSPHRASE", DYDX_API_PASSPHRASE)
                append("DYDX-TIMESTAMP", nowISO)
            }
        }

        // TMP
        val account:String = response.body()
        val acc = Json.decodeFromString<Map<String, DYDXAccountSocket.Account>>(account)
        println(acc["account"])

        /**
         * ORDER TEST
         */
        val positionId = acc["account"]?.positionId
        val symbol = DydxMarket.BTC_USD
        val size = 0.001
        val price = 30000
        val side = OrderSide.BUY
        val type = OrderType.MARKET
        val limitFee = 0.015
        val timeInForce = "FOK"
        val expiration = "2023-09-20T00:00:00.000Z"
        val clientId = UUID.randomUUID().toString()
        println("*** Creating order to $side $size $symbol at $price and type is $type ***")

        val starkPrivateKey = System.getenv("STARK_PRIVATE_KEY")
        val startkPrivateKeyInt = BigInteger(starkPrivateKey, 16)

        val order = Order(positionId, size.toString(), limitFee.toString(), symbol, StarkwareOrderSide.BUY, expiration)
        val orderWithPrice = OrderWithClientIdWithPrice(order, clientId, price.toString())
        val starkwareOrder = StarkwareOrderConverter().fromOrderWithClientId(orderWithPrice, NetworkId.GOERLI)
        val starkHashCalculator = StarkHashCalculator(PedersonHash(ConstantPoints.POINTS[0]))
        val signature = EcSigner(StarkCurve.getInstance()).sign(startkPrivateKeyInt, starkHashCalculator.calculateHash(starkwareOrder))

        val data = mapOf(
            "market" to symbol.toString(),
            "side" to side.toString(),
            "type" to type.toString(),
            "timeInForce" to timeInForce,
            "size" to size.toString(),
            "price" to price.toString(),
            "limitFee" to limitFee.toString(),
            "expiration" to expiration,
            "postOnly" to "false",
            "clientId" to clientId,
            "signature" to signature.toString(),
            "reduceOnly" to "false"
        )

        println("Order JSON:" + jsonStringify(data))

        val nowISOOrder = nowISO()
        val sigOrder = sign(ordersURL, "POST", nowISOOrder, data)

        val responseOrder: HttpResponse = client.post(baseURL + ordersURL) {
            contentType(ContentType.Application.Json)
            setBody(data)
            headers {
                append("DYDX-API-KEY", DYDX_API_KEY)
                append("DYDX-SIGNATURE", sigOrder)
                append("DYDX-PASSPHRASE", DYDX_API_PASSPHRASE)
                append("DYDX-TIMESTAMP", nowISOOrder)
            }
        }

        // print response
        val orderResponse:String = responseOrder.body()
        println(orderResponse)

    } catch(e: Exception) {
        e.printStackTrace()
    }
}
