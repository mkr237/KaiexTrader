package kaiex.model

import kotlinx.serialization.Serializable

enum class OrderType {
    MARKET,
    LIMIT,
    STOP,
    TRAILING_STOP,
    TAKE_PROFIT
}

enum class OrderSide {
    BUY,
    SELL
}

enum class PositionSide {
    LONG,
    SHORT
}

enum class OrderStatus {
    PENDING,
    OPEN,
    FILLED,
    CANCELED,
    UNTRIGGERED
}

enum class OrderTimeInForce {
    GTT,
    FOK,
    IOC
}


@Serializable
data class CreateOrder(val orderId:String,
                       val exchange:String,
                       val symbol: String,
                       val type:OrderType,
                       val side:OrderSide,
                       val price: Float,
                       val size: Float,
                       val timeInForce:OrderTimeInForce,
                       val reduceOnly: Boolean,
                       val createdAt: Long)

@Serializable
data class OrderUpdate(val orderId: String,
                       val exchangeId: String,
                       val accountId:String,
                       val symbol: String,
                       val type:OrderType,
                       val side:OrderSide,
                       val price: Float,
                       val size: Float,
                       val remainingSize: Float,
                       val status: OrderStatus,
                       val timeInForce:OrderTimeInForce,
                       val createdAt: Long,
                       val expiresAt: Long
)

@Serializable
data class OrderFill(val fillId: String,
                     val orderId: String,
                     val symbol: String,
                     val type:OrderType,
                     val side:OrderSide,
                     val price: Float,
                     val size: Float,
                     val fee: Float,
                     val createdAt: Long,
                     val updatedAt: Long
)

@Serializable
data class Position(val positionId:String,
                    val symbol: String,
                    val side:PositionSide,
                    val avgEntryPrice: Float,
                    val avgExitPrice: Float,
                    val size: Float,
                    val pnl: Float,
                    val createdAt:Long,
                    val closedAt:Long)

interface OrderService {
    fun createOrder(symbol: String, type:OrderType, side:OrderSide, price: Float, size: Float)
}