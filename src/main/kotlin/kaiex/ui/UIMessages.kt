package kaiex.ui

import kaiex.model.OrderFill
import kaiex.model.OrderUpdate
import kaiex.model.Position
import kotlinx.serialization.Serializable

@Serializable
sealed class Message {
    abstract val sequenceNumber:Long
}

@Serializable
data class StrategyDescriptorMessage(
    override val sequenceNumber: Long,
    val type:String = "kaiex.ui.StrategyChartConfigMessage",  // TODO why?
    val contents: StrategyChartConfig
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