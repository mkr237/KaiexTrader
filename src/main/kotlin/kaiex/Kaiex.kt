package kaiex

import kaiex.strategy.MACDStrategy
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Kaiex : KoinComponent {
    private val log:Logger = LoggerFactory.getLogger(javaClass.simpleName)

    suspend fun start() {

        log.info("Launching strategies")

        // TODO - if one fails, they all stop!
        coroutineScope {
            launch { MACDStrategy("BTC-USD", 12, 26).start() }
            launch { MACDStrategy("BTC-USD", 10, 16).start() }
            launch { MACDStrategy("ETH-USD", 12, 26).start() }
        }

        log.info("All strategies complete")
    }
}