package kaiex.core

import kaiex.ui.dsl.Chart
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val format = Json { encodeDefaults = true }

class ReportManager : KoinComponent {
    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    val charts = mutableMapOf<String, Chart>()

    fun addChart(chart: Chart) {
        charts[chart.name] = chart
    }
}