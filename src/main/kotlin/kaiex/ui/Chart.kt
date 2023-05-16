package kaiex.ui

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed interface Series

@Serializable
data class LineSeries (
    val name: String,
    val data: MutableList<Double> = mutableListOf()
): Series {
    fun update(value: Double) {
        data.add(value)
    }
}

@Serializable
data class CandleSeries (
    val name: String,
    val data: MutableList<List<Double>> = mutableListOf()
): Series {
    fun update(open: Double, high: Double, low: Double, close: Double) {
        data.add(listOf(open, high, low, close))
    }
}

@Serializable
class Chart (
    val name: String,
    val timestamps: MutableList<Long> = mutableListOf(),
    val series: MutableList<Series> = mutableListOf()
) {
    fun createLineSeries(name: String) = LineSeries(name).also { series.add(it) }
    fun createCandleSeries(name: String) = CandleSeries(name).also { series.add(it) }
    fun updateTime(timestamp: Long) { timestamps.add(timestamp) }
}

fun main() {
    val chart  = Chart("test", mutableListOf())
    val s1 = chart.createCandleSeries("1m")
    val s2 = chart.createLineSeries("macd20")
    val s3 = chart.createLineSeries("macd26")
    val s4 = chart.createLineSeries("position")

    chart.updateTime(Instant.now().epochSecond)
    s1.update(100.0, 120.0, 90.0, 110.0)
    s2.update(105.0)
    s3.update(101.0)
    s4.update(0.0)

    chart.updateTime(Instant.now().epochSecond + 60)
    s1.update(110.0, 115.0, 100.0, 105.0)
    s2.update(103.0)
    s3.update(99.0)
    s4.update(0.02)

    chart.updateTime(Instant.now().epochSecond + 120)
    s1.update(105.0, 115.0, 95.0, 100.0)
    s2.update(102.0)
    s3.update(92.0)
    s4.update(-0.02)

    chart.updateTime(Instant.now().epochSecond + 180)
    s1.update(95.0, 105.0, 90.0, 100.0)
    s2.update(101.0)
    s3.update(98.0)
    s4.update(0.04)

    chart.updateTime(Instant.now().epochSecond + 240)
    s1.update(98.0, 105.0, 95.0, 100.0)
    s2.update(100.0)
    s3.update(101.0)
    s4.update(0.0)

    println(chart.timestamps)
}