package kaiex.strategy

import kaiex.core.AccountManager
import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.core.RiskManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class Strategy : KoinComponent {
    protected val md : MarketDataManager by inject()
    protected val om : OrderManager by inject()
    protected val am : AccountManager by inject()
    protected val rm : RiskManager by inject()
}