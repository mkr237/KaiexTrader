package kaiex.strategy

import com.kaiex.model.Trade
import kaiex.core.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class MACDStrategy(val symbol: String): KoinComponent {
    private val log: org.slf4j.Logger = LoggerFactory.getLogger("$javaClass ($symbol)")

    private val mdm : MarketDataManager by inject()
    private val om : OrderManager by inject()
    private val am : AccountManager by inject()
    private val rm : RiskManager by inject()

    suspend fun start() {
        val tradeBroadcaster:EventBroadcaster<Trade> = mdm.subscribeTrades(symbol)
        tradeBroadcaster.listenForEvents().collect { event ->
            log.info(event.toString())
        }
    }
}
