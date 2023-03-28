package kaiex.indicator

class EMA(private val period: Int) {

    private var value: Double = 0.0
    private var alpha: Double = 0.0

    fun update(price: Double) {
        if (value == 0.0) {
            value = price
            alpha = 2.0 / (period + 1.0)
        } else {
            value = alpha * price + (1 - alpha) * value
        }
    }

    fun getValue(): Double {
        return value
    }
}
