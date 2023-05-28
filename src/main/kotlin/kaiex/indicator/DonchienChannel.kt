package kaiex.indicator

import kaiex.model.Candle

class DonchianChannel(private val lookbackPeriod: Int) {
    private val candles = mutableListOf<Candle>()
    private var upperBand: Double? = null
    private var lowerBand: Double? = null
   fun update(candle: Candle) {
        candles.add(candle)

        if (candles.size > lookbackPeriod) {
            candles.removeAt(0)
        }

        if (candles.size == lookbackPeriod) {
            upperBand = candles.map { it.high.toDouble() }.maxOrNull()
            lowerBand = candles.map { it.low.toDouble() }.minOrNull()
        }
    }

    fun getUpper() = upperBand
    fun getLower() = lowerBand
}