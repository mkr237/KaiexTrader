package com.kaiex

import com.kaiex.services.dydx.DYDXExchangeService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Kaiex : KoinComponent {
    private val log:Logger = LoggerFactory.getLogger(javaClass)
    private val dydxExchangeService : DYDXExchangeService by inject()

    suspend fun start() = coroutineScope {
        log.info("Subscribing to trades")
        dydxExchangeService.subscribeToTrades("BTC-USD").onEach { trade ->
            log.info(trade.toString())
        }.launchIn(this)

        log.info("Subscribing to orderbooks")
        dydxExchangeService.subscribeToOrderBook("BTC-USD").onEach { ob ->
            log.info(ob.toString())
        }.launchIn(this)
    }
}