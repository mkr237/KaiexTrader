import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kaiex.exchange.dydx.getAccountId
import kaiex.exchange.dydx.nowISO
import kaiex.exchange.dydx.sign
import kotlinx.serialization.json.Json

suspend fun main() {

    val ETHEREUM_ADDRESS = System.getenv("ETHEREUM_ADDRESS")
    val DYDX_API_KEY = System.getenv("DYDX_API_KEY")
    val DYDX_API_PASSPHRASE = System.getenv("DYDX_API_PASSPHRASE")
    val DYDX_ACCOUNT_ID = getAccountId(ETHEREUM_ADDRESS)

    val url = "https://api.stage.dydx.exchange/v3/accounts/${DYDX_ACCOUNT_ID}"
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

        val nowISO = nowISO()
        val sig = sign("/v3/accounts/${DYDX_ACCOUNT_ID}",
                           "GET",
                                   nowISO,
                                   mapOf())

        val response: HttpResponse = client.get(url) {
            headers {
                append("DYDX-API-KEY", DYDX_API_KEY)
                append("DYDX-SIGNATURE", sig)
                append("DYDX-PASSPHRASE", DYDX_API_PASSPHRASE)
                append("DYDX-TIMESTAMP", nowISO)
            }
        }

        // TMP
        val account:String = response.body()
        println(account)

    } catch(e: Exception) {
        e.printStackTrace()
    }
}
