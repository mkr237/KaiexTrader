package kaiex.reporting

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed interface ChartData
@Serializable
data class BarData(val time: Long, val open: Double, val high: Double, val low: Double, val close: Double): ChartData
@Serializable
data class LineData(val time: Long, val value: Double): ChartData

@Serializable
sealed interface Series<T : ChartData> {
    val dataList: MutableList<T>
    fun update(data: T)
}

@Serializable
data class LineSeries(val name: String,
                 val color:SeriesColor,
                 val lineWidth: Int,
                 val pane: Int) : Series<LineData> {

    override val dataList = mutableListOf<LineData>()
    override fun update(data: LineData) {
        dataList.add(data)
    }
}

@Serializable
data class AreaSeries(val name: String,
                      val color:SeriesColor,
                      val pane: Int) : Series<LineData> {

    override val dataList = mutableListOf<LineData>()
    override fun update(data: LineData) {
        dataList.add(data)
    }
}

@Serializable
data class CandlestickSeries(val name: String,
                        val upColor:SeriesColor,
                        val downColor:SeriesColor,
                        val pane: Int) : Series<BarData> {

    override val dataList = mutableListOf<BarData>()
    override fun update(data: BarData) {
        dataList.add(data)
    }
}

@Serializable
data class HistogramSeries(val name: String,
                          val color:SeriesColor,
                          val pane: Int) : Series<LineData> {

    override val dataList = mutableListOf<LineData>()
    override fun update(data: LineData) {
        dataList.add(data)
    }
}

@Serializable
enum class SeriesColor(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF),
    WHITE(0xFFFFFF)
}

@Serializable
class Chart(val name: String) {
    val seriesList = mutableSetOf<Series<out ChartData>>()

    fun createLineSeries(name: String,
                         color:SeriesColor = SeriesColor.WHITE,
                         lineWidth: Int = 2,
                         pane: Int = 0): LineSeries {
        val series = LineSeries(name, color, lineWidth, pane)
        seriesList.add(series)
        return series
    }

    fun createAreaSeries(name: String,
                         color:SeriesColor = SeriesColor.WHITE,
                         pane: Int = 0): AreaSeries {
        val series = AreaSeries(name, color, pane)
        seriesList.add(series)
        return series
    }

    fun createCandlestickSeries(name: String,
                                upColor:SeriesColor = SeriesColor.GREEN,
                                downColor:SeriesColor = SeriesColor.RED,
                                pane: Int = 0): CandlestickSeries {
        val series = CandlestickSeries(name, upColor, downColor, pane)
        seriesList.add(series)
        return series
    }

    fun createHistogramSeries(name: String,
                              color:SeriesColor = SeriesColor.WHITE,
                              pane: Int = 0): HistogramSeries {
        val series = HistogramSeries(name, color, pane)
        seriesList.add(series)
        return series
    }
}

fun main() {
    val chart = Chart("MyChart")

    val series1 = chart.createLineSeries("Line1")
    series1.update(LineData(Instant.now().epochSecond, 1.4))
    series1.update(LineData(Instant.now().epochSecond, 1.4))
    series1.update(LineData(Instant.now().epochSecond, 1.5))
    series1.update(LineData(Instant.now().epochSecond, 1.6))
    series1.update(LineData(Instant.now().epochSecond, 1.7))

    val series2 = chart.createCandlestickSeries("Candle1")
    series2.update(BarData(Instant.now().epochSecond, 1.2, 1.4, 0.9, 1.1))
    series2.update(BarData(Instant.now().epochSecond, 1.1, 1.6, 0.7, 1.0))
    series2.update(BarData(Instant.now().epochSecond, 1.0, 1.1, 0.9, 1.2))
}
