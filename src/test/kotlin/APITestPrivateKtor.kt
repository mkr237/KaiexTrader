import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kaiex.util.UUID5
import kotlinx.serialization.json.Json
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString as myJsonEncode

// for UUID5
val NAMESPACE: UUID = UUID.fromString("0f9da948-a6fb-4c45-9edc-4685c3f3317d")

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

private fun nowISO(): String {
    val tz = TimeZone.getTimeZone("UTC")
    val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    df.timeZone = tz
    return df.format(Date())
}

fun sign(
    requestPath: String,
    method: String,
    isoTimestamp: String,
    data: Map<String, String>
): String {

    val DYDX_API_SECRET = System.getenv("DYDX_API_SECRET")
    val messageString = isoTimestamp + method + requestPath + if (data.isNotEmpty()) jsonStringify(data) else ""
    val hmac = Mac.getInstance("HmacSHA256")
    val secret = SecretKeySpec(
        Base64.getUrlDecoder().decode(encodeUtf8(DYDX_API_SECRET)),
        "HmacSHA256"
    )
    hmac.init(secret)
    val digest = hmac.doFinal(messageString.toByteArray())

    return Base64.getUrlEncoder().encodeToString(digest)
}

fun jsonStringify(obj: Map<String, String>): String {
    val json = Json { encodeDefaults = true }
    return json.myJsonEncode(obj)
}

fun encodeUtf8(str: String): ByteArray {
    return str.toByteArray(charset("UTF-8"))
}

fun getAccountId(address: String, accountNumber: Int = 0): String {
    val userId = UUID5.fromUTF8(NAMESPACE, address.lowercase()).toString()
    return UUID5.fromUTF8(NAMESPACE, userId + accountNumber.toString()).toString()
}
