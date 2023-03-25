package kaiex.model

enum class Type {
    MARKET,
    LIMIT
}

interface OrderService {
    fun createOrder(symbol: String, type:Type, side:Side, price: Float, size: Float)
}