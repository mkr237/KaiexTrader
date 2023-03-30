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

    private val strategies:MutableMap<String, MutableList<StrategyReport>> = mutableMapOf()

    fun submitStrategyReport(snapshot: StrategyReport) {
        log.info("Received strategy snapshot: $snapshot")
        if(!strategies.containsKey(snapshot.strategyId)) {

            // add to strategy tracker
            log.info("Tracking new strategy: ${snapshot.strategyId}")
            strategies[snapshot.strategyId] = mutableListOf(snapshot)

            // add a socket to the UI server
            uiServer.addDataStream("/${snapshot.strategyId}") { seq ->
                log.info("UI has asked for an update (seq=$seq) for: ${snapshot.strategyId}")

                // TODO - Review this code!
                val range = IntRange(seq, strategies[snapshot.strategyId]!!.size - 1)
                val history = strategies[snapshot.strategyId]?.slice(range)

                if(history!!.isNotEmpty())
                    DataPacket(strategies[snapshot.strategyId]!!.size - 1, format.myJsonEncode(history))
                else
                    null
            }

        } else {

            // update existing report
            log.info("Adding report to strategy history: $snapshot")
            strategies[snapshot.strategyId]!!.add(snapshot)
        }
    }
}