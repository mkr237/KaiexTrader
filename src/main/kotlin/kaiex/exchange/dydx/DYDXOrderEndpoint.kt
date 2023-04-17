package kaiex.exchange.dydx

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kaiex.core.format
import kaiex.util.Resource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlinx.serialization.encodeToString as myJsonEncode
import com.fersoft.signature.StarkSigner


class DYDXOrderEndpoint(): DYDXEndpoint<Unit> {

    //@Serializable
    //sealed class Message {}

    @Serializable
    data class OrderRequest(
        val market: String,
        val side: String,
        val type: String,
        val size: String,
        val price: String,
        val timeInForce: String = "GTT",
        val postOnly: Boolean = false,
        val clientId: String = UUID.randomUUID().toString(),
        val accountId: String = "0"
    )//:Message()

    @Serializable
    data class OrderResponse(
        val orderId: String
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
        install(Logging)
        install(ContentNegotiation) {
            json(customJson)
        }
    }

    override suspend fun get(url: String): Resource<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun post(url: String, data: Map<String, String>): Resource<Unit> {

        log.info("Sending order to $url: $data")

        try {

            val nowISO = nowISO()
            val sig = sign(url, "POST", nowISO, data)

            val response: HttpResponse = client.post("https://api.stage.dydx.exchange/v3/orders") {
                contentType(ContentType.Application.Json)
                setBody(data)
                headers {
                    append("DYDX-API-KEY", DYDX_API_KEY)
                    append("DYDX-SIGNATURE", sig)
                    append("DYDX-PASSPHRASE", DYDX_API_PASSPHRASE)
                    append("DYDX-TIMESTAMP", nowISO)
                }
            }

            // TODO check response for success/failure

            // TMP
            log.info(response.body())

            return Resource.Success(Unit)

        } catch(e: Exception) {
            e.printStackTrace()
            return Resource.Error(e.localizedMessage ?: "Failed to send order")
        }
    }
}