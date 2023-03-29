package kaiex.strategy

import kaiex.core.*
import kaiex.model.Order
import kaiex.model.Position
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

abstract class Strategy(val strategyId: String) : KoinComponent {

    @Serializable
    data class StrategySnapshot(var strategyId: String = "",
                                var pnl: Double = 0.0,
                                var orders: List<Order> = emptyList(),
                                var positions: List<Position> = emptyList(),
                                var marketData: Map<String, Double> = emptyMap(),
                                var timeStamp: Long = Instant.now().epochSecond)

    protected val marketDataManager : MarketDataManager by inject()
    protected val orderManager : OrderManager by inject()
    protected val accountManager : AccountManager by inject()
    protected val riskManager : RiskManager by inject()
    protected val reportManager : ReportManager by inject()
}