package kaiex.indicator

class MACD(private val fastPeriod: Int, private val slowPeriod: Int, private val signalPeriod: Int):Indicator {

    private var fastMA: EMA = EMA(fastPeriod)
    private var slowMA: EMA = EMA(slowPeriod)
    private var signalMA: EMA = EMA(signalPeriod)

    override fun update(price: Double) {
        fastMA.update(price)
        slowMA.update(price)

        val macdValue = fastMA.value - slowMA.value
        signalMA.update(macdValue)
    }

    fun getMACDLine(): Double {
        return fastMA.value - slowMA.value
    }

    fun getSignalLine(): Double {
        return signalMA.value
    }

    fun getHistogram(): Double {
        return getMACDLine() - getSignalLine()
    }
}