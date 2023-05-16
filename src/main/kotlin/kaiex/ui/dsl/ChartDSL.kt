package kaiex.ui.dsl

import com.google.gson.Gson
import java.time.Instant

sealed interface Series {
    val data: MutableList<Any>
}

data class TimestampSeries(
    override val data: MutableList<Any> = mutableListOf()
) : Series

data class ValueSeries(
    var color: String? = null,
    override val data: MutableList<Any> = mutableListOf()
) : Series

data class CandleSeries(
    var upColor: String? = null,
    var downColor: String? = null,
    override val data: MutableList<Any> = mutableListOf()
) : Series

data class ChartData(
    var series: MutableMap<String, Series> = mutableMapOf()
)

enum class SeriesColor(val rgb: String) {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#2196F3"),
    ORANGE("#FC6C02"),
    GREY("#CCCCCC")
}

data class Chart(val name: String) {
    private val chartData = ChartData()

    init {
        val timestampSeries = TimestampSeries()
        chartData.series["Timestamps"] = timestampSeries
    }

    fun valueSeries(name: String, block: ValueSeries.() -> Unit) {
        val valueSeries = ValueSeries().apply(block)
        chartData.series[name] = valueSeries
    }

    fun candleSeries(name: String, block: CandleSeries.() -> Unit) {
        val candleSeries = CandleSeries().apply(block)
        chartData.series[name] = candleSeries
    }

    fun update(timestamp: Long, block: ChartUpdater.() -> Unit) {
        chartData.series["Timestamps"]?.data?.add(timestamp)
        val updater = ChartUpdater(chartData.series)
        updater.block()
    }

    fun toJson(): String = Gson().toJson(chartData)

    inner class ChartUpdater(private val seriesData: MutableMap<String, Series>) {

        operator fun String.invoke(value: Any) {
            when (val series = seriesData[this]) {
                is ValueSeries -> series.data.add(value as Double)
                is CandleSeries -> series.data.add(value as List<Double>)
                else -> {

                }
            }
        }
    }
}

fun createChart(name:String, block: Chart.() -> Unit): Chart {
    val chart = Chart(name)
    chart.block()
    return chart
}

fun main() {
    val chart = createChart("Default") {
        valueSeries("MACD") {
            color = "FF0000"
        }
        valueSeries("Signal") {
            color = "00FF00"
        }
        candleSeries("Candles") {
            upColor = "#00FF00"
            downColor = "#FF0000"
        }
    }

    val timestamp1 = Instant.parse("2023-01-01T00:00:00Z").epochSecond
    chart.update(timestamp1) {
        "MACD"(105.0)
        "Signal"(101.0)
        "Candles"(listOf(100.0, 120.0, 90.0, 110.0))
    }

    val timestamp2 = Instant.parse("2023-01-01T01:00:00Z").epochSecond
    chart.update(timestamp2) {
        "MACD"(106.0)
        "Signal"(102.0)
        "Candles"(listOf(101.0, 121.0, 91.0, 111.0))
    }

    println(chart.toJson())
}