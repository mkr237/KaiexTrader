package kaiex.strategy

import kaiex.model.MarketDataSnapshot
import kaiex.model.OrderUpdate
import kaiex.ui.StrategyConfig
import kotlinx.coroutines.Job
import java.time.Instant

interface KaiexStrategy {
//    suspend fun onCreate()
//    fun onMarketData(snapshot: Map<String, MarketDataSnapshot>)
//    fun onOrderUpdate(update: OrderUpdate)
//    fun onDestroy()

    suspend fun start()
    fun stop()
}