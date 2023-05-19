package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.StrategyConfig
import java.time.Instant

interface KaiexStrategy {
    suspend fun onCreate()
    suspend fun onMarketData(snapshot: Map<String, MarketDataSnapshot>)
    suspend fun onOrderUpdate(update: OrderUpdate)
    suspend fun onDestroy()
}