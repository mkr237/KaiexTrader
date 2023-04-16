package kaiex.strategy

import kaiex.core.*
import kaiex.model.*
import kaiex.ui.UIServer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import toCandles
import java.time.Instant

abstract class Strategy(val strategyId: String) : KoinComponent {

    private val accountManager : AccountManager by inject()

    private val marketDataManager : MarketDataManager by inject()
    protected val orderManager : OrderManager by inject()
    protected val riskManager : RiskManager by inject()
    protected val reportManager : ReportManager by inject()
    protected val uiServer : UIServer by inject()

    protected val orders : MutableMap<String, OrderUpdate> = mutableMapOf()
    protected val fills : MutableMap<String, OrderFill> = mutableMapOf()
    protected val positions : MutableMap<String, Position> = mutableMapOf()
    abstract val config : StrategyChartConfig

    abstract suspend fun onStart()
    abstract suspend fun onUpdate()
    abstract suspend fun onStop()

    suspend fun subscribeCandles(symbol: String, onCandle: (Candle) -> Unit) {
        coroutineScope {
            async {
                marketDataManager.subscribeTrades(symbol).listenForEvents().toCandles()
                    .collect { candle -> onCandle(candle) }
            }
        }
    }

    suspend fun start() {

        // register with the UI server
        uiServer.register(config)

        //
        coroutineScope {

            handleAccountUpdate(AccountUpdate(strategyId, emptyList(), emptyList(), emptyList()))

            async {
                accountManager.subscribeAccountUpdates("0").listenForEvents().collect { update ->
                    handleAccountUpdate(update)
                    onUpdate()
                }
            }

            onStart()
        }
    }

    private fun handleAccountUpdate(update: AccountUpdate) {
        //log.info("Received Account Update: $update")
        update.orders.forEach { order -> orders[order.orderId] = order }
        update.fills.forEach { fill -> fills[fill.fillId] = fill }
        update.positions.forEach { position -> positions[position.positionId] = position }
        uiServer.send(
            StrategySnapshot(
                strategyId,
                Instant.now().epochSecond,
                86.2f,
                23,
                54f,
                22f,
                1.31f,
                emptyMap(),
                orders,
                fills,
                positions))
    }
}

@Serializable
data class StrategyChartConfig(
    val strategyId: String,
    val strategyName: String,
    val strategyDescription: String,
    val chartConfig: List<ChartSeriesConfig>
)

@Serializable
data class ChartSeriesConfig(
    val id: String,
    val seriesType: String,
    val pane: Int,
    val color: String
)

@Serializable
data class StrategySnapshot(val strategyId:String,
                            val timestamp: Long,
                            val profitAndLoss: Float,
                            val numberOfTrades: Int,
                            val winLossRatio: Float,
                            val maxDrawdown: Float,
                            val sharpRatio: Float,
                            val marketData: Map<String, Float>,
                            val orders: Map<String, OrderUpdate>,
                            val fills: Map<String, OrderFill>,
                            val positions: Map<String, Position>)

@Serializable
data class StrategyMarketDataUpdate(
    val strategyId: String,
    val timestamp: Long,
    val updates: List<SeriesUpdate>
)

@Serializable
sealed class SeriesUpdate {
    abstract val id: String

    @Serializable
    data class NumericUpdate(
        override val id: String,
        val value: Double
    ) : SeriesUpdate()

    @Serializable
    data class CandleUpdate(
        override val id: String,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double
    ) : SeriesUpdate()
}