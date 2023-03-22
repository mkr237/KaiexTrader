import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Market(
    val market: String,
    val status: String,
    val baseAsset: String,
    val quoteAsset: String,
    val stepSize: String,
    val tickSize: String,
    val indexPrice: String,
    val oraclePrice: String,
    val priceChange24H: String,
    val nextFundingRate: String,
    val nextFundingAt: String,
    val minOrderSize: String,
    val type: String,
    val initialMarginFraction: String,
    val maintenanceMarginFraction: String,
    val transferMarginFraction: String,
    val volume24H: String,
    val trades24H: String,
    val openInterest: String,
    val incrementalInitialMarginFraction: String,
    val incrementalPositionSize: String,
    val maxPositionSize: String,
    val baselinePositionSize: String,
    val assetResolution: String,
    val syntheticAssetId: String
)

@Serializable
data class Markets(
    val markets:Map<String, Market>
)
suspend fun main() {

    val url = "https://api.stage.dydx.exchange/v3/markets"
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

        val response: HttpResponse = client.get(url) {}
        val markets:Markets = response.body()
        println(markets.markets["BTC-USD"])

    } catch(e: Exception) {
        e.printStackTrace()
    }
}