package kaiex

import kaiex.strategy.DumbStrategy
import kaiex.strategy.MACDStrategy
import kaiex.strategy.OrderBookWatcher
import kaiex.ui.UIServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Kaiex : KoinComponent {
    private val log:Logger = LoggerFactory.getLogger(javaClass.simpleName)

    //val s1 = MACDStrategy("BTC-USD", 12, 26)

    suspend fun start() {

        log.info("Starting UI Server")
        val uiServer : UIServer by inject()
        GlobalScope.launch {
            uiServer.start()
        }

        delay(2000)     // TODO fix - must wait until UI server has started

        // TODO - if one fails, they all stop!
        log.info("Launching strategies")
        coroutineScope {
            //launch { MACDStrategy("BTC-USD", 12, 26).start() }
            launch { DumbStrategy("BTC-USD", 30000L).start() }
//            launch { MACDStrategy("BTC-USD", 10, 16).start() }
//            launch { MACDStrategy("ETH-USD", 12, 26).start() }
//            launch { MACDStrategy("LINK-USD", 12, 26).start() }
//            launch { MACDStrategy("XMR-USD", 12, 26).start() }
//            launch { MACDStrategy("LTC-USD", 12, 26).start() }
//            launch { MACDStrategy("EOS-USD", 12, 26).start() }
        }

        log.info("All strategies complete")
    }
}