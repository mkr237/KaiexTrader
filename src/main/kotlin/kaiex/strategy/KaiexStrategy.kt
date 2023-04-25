package kaiex.strategy

import kaiex.model.OrderUpdate
import kaiex.ui.StrategyConfig

interface KaiexStrategy {
    suspend fun onCreate(config: StrategyConfig)
    fun onMarketData()
    fun onOrderUpdate(update: OrderUpdate)
    suspend fun onDestroy()
}