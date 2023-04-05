package kaiex.core

import kaiex.strategy.StrategyReport
import kaiex.ui.DataPacket
import kaiex.ui.UIServer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlinx.serialization.encodeToString as myJsonEncode

val format = Json { encodeDefaults = true }

class ReportManager : KoinComponent {
    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val uiServer : UIServer by inject()

    // TODO - this grows unbounded!
    private val strategies:MutableMap<String, MutableList<StrategyReport>> = mutableMapOf()

    // TODO do we even need this?
    private var index = 0

    fun submitStrategyReport(snapshot: StrategyReport) {
        if(!strategies.containsKey(snapshot.strategyId)) {

            // add to strategy tracker
            log.info("Tracking new strategy: ${snapshot.strategyId}")
            strategies[snapshot.strategyId] = mutableListOf(snapshot)

            // create e UI socket
            uiServer.createSocket("/${snapshot.strategyId}")

        } else {

            // add to history
            log.debug("Adding report to strategy history: $snapshot")
            strategies[snapshot.strategyId]!!.add(snapshot)
        }

        uiServer.sendData("/${snapshot.strategyId}", DataPacket(index++, format.myJsonEncode(snapshot)))
    }
}