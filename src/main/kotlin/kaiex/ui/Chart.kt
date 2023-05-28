package kaiex.ui

import com.google.gson.Gson

enum class SeriesType {
    CANDLE,
    LINE,
    BAR
}

enum class SeriesColor(val rgb: String) {
    RED("#FF0000"),
    GREEN("#00FF00"),
    BLUE("#2196F3"),
    ORANGE("#FC6C02"),
    GREY("#CCCCCC");

    override fun toString() = rgb
}

sealed interface Series {
    val type: SeriesType
    val data: MutableList<Any>
}

enum class LineShape { linear, spline, hv, vh, hvh, vhv }

data class LineSeries(
    override val type:SeriesType = SeriesType.LINE,
    var color: String? = null,
    var shape: LineShape? = LineShape.linear,
    override val data: MutableList<Any> = mutableListOf(),
) : Series

data class BarSeries(
    override val type:SeriesType = SeriesType.BAR,
    var upColor: String? = null,
    var downColor: String? = null,
    override val data: MutableList<Any> = mutableListOf(),
) : Series

data class CandleSeries(
    override val type:SeriesType = SeriesType.CANDLE,
    var upColor: String? = null,
    var downColor: String? = null,
    override val data: MutableList<Any> = mutableListOf(),
) : Series

class Chart(val name: String) {
    private val timestamps: MutableList<Long> = mutableListOf()
    private val plots = mutableListOf<Plot>()

    fun plot(name: String, block: Plot.() -> Unit) {
        val plot = Plot(name)
        plot.block()
        plots.add(plot)
    }

    fun update(timestamp: Long, block: ChartUpdater.() -> Unit) {
        val updater = ChartUpdater(timestamp)
        updater.block()
        timestamps.add(timestamp)
        plots.forEach { plot ->
            plot.seriesData.forEach { (seriesName, series) ->
                updater.updatedValues[seriesName]?.let { updatedValue ->
                    series.data.add(updatedValue)
                }
            }
        }
    }

    fun toJson(): String = Gson().toJson(this)
}

data class ChartUpdater(val timestamp: Long) {
    val updatedValues: MutableMap<String, Any> = mutableMapOf()
    fun set(seriesName: String, value: Any) {
        updatedValues[seriesName] = value
    }
}

data class Plot(val name: String) {
    var height: Double = 0.0
    var seriesData: MutableMap<String, Series> = mutableMapOf()

    fun lineSeries(name: String, block: LineSeries.() -> Unit) {
        val series = LineSeries().apply(block)
        seriesData[name] = series
    }

    fun barSeries(name: String, block: BarSeries.() -> Unit) {
        val series = BarSeries().apply(block)
        seriesData[name] = series
    }

    fun candleSeries(name: String, block: CandleSeries.() -> Unit) {
        val series = CandleSeries().apply(block)
        seriesData[name] = series
    }
}

fun createChart(name:String, block: Chart.() -> Unit): Chart {
    val chart = Chart(name)
    chart.block()
    return chart
}
