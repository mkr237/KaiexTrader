package kaiex.model

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

enum class PositionStatus {
    OPEN,
    CLOSED,
    LIQUIDATED
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

enum class OrderRole {
    MAKER,
    TAKER
}

// TODO the timestamp fields should be changed to Instant but that means the UI messages can't use them directly anymore

data class CreateOrder(
    val orderId:String,
    val exchange:String,
    val symbol: String,
    val type:OrderType,
    val side:OrderSide,
    val price: Float,
    val size: Float,
    val limitFee: Float,
    val timeInForce:OrderTimeInForce,
    val postOnly:Boolean,
    val reduceOnly: Boolean,
    val createdAt: Long)

data class OrderUpdate(
    val orderId: String,
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

data class OrderFill(
    val fillId: String,
    val orderId: String,
    val symbol: String,
    val type:OrderType,
    val side:OrderSide,
    val price: Float,
    val size: Float,
    val fee: Float,
    val role: OrderRole,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun isBuy() = side == OrderSide.BUY
    fun isSell() = side == OrderSide.SELL
}

data class Position(
    val positionId:String,
    val accountId: String,
    val symbol: String,
    val side: PositionSide,
    val status: PositionStatus,
    val size: Float,
    val maxSize: Float,
    val entryPrice: Float,
    val exitPrice: Float,
    val openTransactionId: String,
    val closeTransactionId: String,
    val lastTransactionId: String,
    val closedAt: Long,
    val updatedAt: Long,
    val createdAt: Long,
    val sumOpen: Float,
    val sumClose: Float,
    val netFunding: Float,
    val unrealisedPnl: Float,
    val realizedPnl: Float)
{
    fun isLong() = side == PositionSide.LONG
    fun isShort() = side == PositionSide.SHORT
    fun isOpen() = status == PositionStatus.OPEN
}

interface OrderService {
    fun createOrder(order: CreateOrder):Result<OrderUpdate>
    // TODO cancel/amend etc
}