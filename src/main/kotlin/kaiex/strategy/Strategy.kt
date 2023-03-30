package kaiex.strategy

import kaiex.core.*
import kaiex.model.Order
import kaiex.model.Position
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

abstract class Strategy(val strategyId: String) : KoinComponent {

    protected val marketDataManager : MarketDataManager by inject()
    protected val orderManager : OrderManager by inject()
    protected val accountManager : AccountManager by inject()
    protected val riskManager : RiskManager by inject()
    protected val reportManager : ReportManager by inject()
}