package com.kaiex

import kaiex.strategy.MACDStrategy
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Kaiex : KoinComponent {
    private val log:Logger = LoggerFactory.getLogger(javaClass)

    suspend fun start() {

        log.info("Launching strategies")

        // TODO - if one fails, they all stop!
        coroutineScope {
            launch(CoroutineName("MACD (BTC-USD)")) { MACDStrategy("BTC-USD").start() }
            launch(CoroutineName("MACD (ETH-USD)")) { MACDStrategy("ETH-USD").start() }
            launch(CoroutineName("MACD (BTC-USD)")) { MACDStrategy("BTC-USD").start() }
        }

        log.info("All strategies complete")
    }
}