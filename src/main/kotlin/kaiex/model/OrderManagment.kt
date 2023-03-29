package kaiex.model

import kotlinx.serialization.Serializable

enum class Type {
    MARKET,
    LIMIT
}

enum class OrderStatus {
    PENDING,
    ACCEPTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED
}

@Serializable
data class Order(val orderId:String,
                 val exchange:String,
                 val symbol: String,
                 val type:Type,
                 val side:Side,
                 val price: Float,
                 val size: Float,
                 val status: OrderStatus)

@Serializable
data class Position(val positionId:String,
                    val exchange:String,
                    val symbol: String,
                    val side:Side,
                    val avgPrice: Float,
                    val size: Float,
                    val pnl: Float)

interface OrderService {
    fun createOrder(symbol: String, type:Type, side:Side, price: Float, size: Float)
}