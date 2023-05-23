package kaiex.api

import kotlinx.serialization.Serializable

@Serializable
data class Metrics(
    val pnl: Double,
    val numTrades:Int,
    val winRate:Double,
    val sharpe:Double
)

@Serializable
data class Order(
    val orderId: String,
    val exchangeId: String,
    val accountId:String,
    val symbol: String,
    val type: String,
    val side: String,
    val price: Double,
    val size: Double,
    val remainingSize: Double,
    val status: String,
    val timeInForce: String,
    val createdAt: Long,
    val expiresAt: Long,
    val fills: List<Fill>
)

@Serializable
data class Fill(
    val fillId: String,
    val price: Double,
    val size: Double,
    val fee: Double,
    val role: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class Position(
    val positionId:String,
    val accountId: String,
    val symbol: String,
    val side: String,
    val status: String,
    val size: Double,
    val maxSize: Double,
    val entryPrice: Double,
    val exitPrice: Double,
    val openTransactionId: String,
    val closeTransactionId: String,
    val lastTransactionId: String,
    val closedAt: Long,
    val updatedAt: Long,
    val createdAt: Long,
    val sumOpen: Double,
    val sumClose: Double,
    val netFunding: Double,
    val unrealisedPnl: Double,
    val realizedPnl: Double
)

/*
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
*/