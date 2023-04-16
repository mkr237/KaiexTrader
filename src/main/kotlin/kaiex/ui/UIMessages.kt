package kaiex.ui

import kaiex.strategy.StrategyMarketDataUpdate
import kaiex.strategy.StrategyChartConfig
import kaiex.strategy.StrategySnapshot
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
