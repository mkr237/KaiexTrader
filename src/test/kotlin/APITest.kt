import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val url = "https://api.stage.dydx.exchange/v3/candles"
private val client: HttpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .followRedirects(HttpClient.Redirect.NEVER)
    .connectTimeout(Duration.ofSeconds(20))
    .build()

suspend fun fetchCandles(symbol: String): HttpResponse<String> {
    val request = HttpRequest.newBuilder()
        .timeout(Duration.ofSeconds(20))
        .uri(URI.create(url + "/$symbol"))
        .build()
    val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    return response.await()
}

fun main() {
    runBlocking {
        val result = async (Dispatchers.IO) {
            val data = fetchCandles("BTC-USD")
            data
        }
        val data = result.await()
        println(data.body())
    }
}