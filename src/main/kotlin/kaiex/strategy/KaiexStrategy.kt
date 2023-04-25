package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.StrategyConfig

interface KaiexStrategy {
    suspend fun onCreate(config: StrategyConfig)
    suspend fun onMarketData()
    suspend fun onOrderUpdate(update: OrderUpdate)
    suspend fun onDestroy()
}