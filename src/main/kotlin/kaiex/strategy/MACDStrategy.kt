package kaiex.strategy

import kaiex.model.Trade
import kaiex.core.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class MACDStrategy(val symbol: String, val ma1:Int, val ma2:Int): KoinComponent {
    private val log: org.slf4j.Logger = LoggerFactory.getLogger("${javaClass.simpleName}($symbol,$ma1,$ma2)")

    private val md : MarketDataManager by inject()
    private val om : OrderManager by inject()
    private val am : AccountManager by inject()
    private val rm : RiskManager by inject()

    suspend fun start() {
        val tradeBroadcaster:EventBroadcaster<Trade> = md.subscribeTrades(symbol)
        tradeBroadcaster.listenForEvents().collect { event ->
            log.info(event.toString())
        }
    }
}
