package kaiex.ui

import kaiex.model.OrderFill
import kaiex.model.OrderUpdate
import kotlinx.serialization.Serializable

@Serializable
sealed class Message {
    abstract val sequenceNumber:Long
}

@Serializable
data class StrategyDescriptorMessage(
    override val sequenceNumber: Long,
    val type:String = "kaiex.ui.StrategyChartConfigMessage",  // TODO why?
    val contents: StrategyConfig
):Message()

@Serializable
data class StrategySnapshotMessage(
    override val sequenceNumber: Long,
    val contents: StrategySnapshot
):Message()

@Serializable
data class StrategyMarketDataUpdateMessage(
    override val sequenceNumber: Long,
    val contents: StrategyMarketDataUpdate
):Message()

@Serializable
data class StrategyConfig(
    val strategyId: String = "",
    val strategyType: String = "",
    val strategyDescription: String = "",
    val symbols: List<String> = emptyList(),
    val parameters: Map<String, String> = emptyMap(),
    val chartConfig: List<ChartSeriesConfig> = emptyList()
)

@Serializable
data class ChartSeriesConfig(
    val id: String,
    val seriesType: String,
    val pane: Int,
    val color: String
)

@Serializable
class StrategyPosition(val symbol: String,
                       val positionSize: Float,
                       val avgEntryPrice: Float,
                       val avgExitPrice: Float,
                       val realizedPnl: Float,
                       val unrealizedPnl: Float,
                       val marketPrice: Float)

@Serializable
data class StrategySnapshot(val strategyId:String,
                            val strategyType:String,
                            val symbols:List<String>,
                            val parameters:Map<String, String>,
                            val timestamp: Long,
                            val profitAndLoss: Float,
                            val numberOfTrades: Int,
                            val winLossRatio: Float,
                            val maxDrawdown: Float,
                            val sharpRatio: Float,
                            val marketData: Map<String, Float>,
                            val orders: Map<String, OrderUpdate>,
                            val fills: Map<String, OrderFill>,
                            val positions: Map<String, StrategyPosition>)

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