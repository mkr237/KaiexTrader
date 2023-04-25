package kaiex.indicator

class MACD(private val fastPeriod: Int, private val slowPeriod: Int, private val signalPeriod: Int):Indicator {

    private var fastMA: EMA = EMA(fastPeriod)
    private var slowMA: EMA = EMA(slowPeriod)
    private var signalMA: EMA = EMA(signalPeriod)

    override fun update(price: Double) {
        fastMA.update(price)
        slowMA.update(price)

        val macdValue = fastMA.getValue() - slowMA.getValue()
        signalMA.update(macdValue)
    }

    fun getMACDLine(): Double {
        return fastMA.getValue() - slowMA.getValue()
    }

    fun getSignalLine(): Double {
        return signalMA.getValue()
    }

    fun getHistogram(): Double {
        return getMACDLine() - getSignalLine()
    }
}