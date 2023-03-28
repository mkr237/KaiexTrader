package kaiex.indicator

class MovingAverage(prices: List<Double>, period: Int) {
    val values: List<Double>

    init {
        val values = mutableListOf<Double>()

        for (i in period - 1 until prices.size) {
            val sum = prices.subList(i - period + 1, i + 1).sum()
            values.add(sum / period)
        }

        this.values = values
    }
}

