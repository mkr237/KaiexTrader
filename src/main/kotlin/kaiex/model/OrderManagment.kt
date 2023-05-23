package kaiex.model

import java.math.BigDecimal
import java.time.Instant

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

data class CreateOrder(
    val orderId:String,
    val exchange:String,
    val symbol: String,
    val type:OrderType,
    val side:OrderSide,
    val price: BigDecimal,
    val size: BigDecimal,
    val limitFee: BigDecimal,
    val timeInForce:OrderTimeInForce,
    val postOnly:Boolean,
    val reduceOnly: Boolean,
    val createdAt: Instant)

data class OrderUpdate(
    val orderId: String,
    val exchangeId: String,
    val accountId:String,
    val symbol: String,
    val type:OrderType,
    val side:OrderSide,
    val price: BigDecimal,
    val size: BigDecimal,
    val remainingSize: BigDecimal,
    val status: OrderStatus,
    val timeInForce:OrderTimeInForce,
    val createdAt: Instant,
    val expiresAt: Instant
)

data class OrderFill(
    val fillId: String,
    val orderId: String,
    val symbol: String,
    val type:OrderType,
    val side:OrderSide,
    val price: BigDecimal,
    val size: BigDecimal,
    val fee: BigDecimal,
    val role: OrderRole,
    val createdAt: Instant,
    val updatedAt: Instant
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
    val size: BigDecimal,
    val maxSize: BigDecimal,
    val entryPrice: BigDecimal,
    val exitPrice: BigDecimal,
    val openTransactionId: String,
    val closeTransactionId: String,
    val lastTransactionId: String,
    val closedAt: Instant?,
    val updatedAt: Instant,
    val createdAt: Instant,
    val sumOpen: BigDecimal,
    val sumClose: BigDecimal,
    val netFunding: BigDecimal,
    val unrealisedPnl: BigDecimal,
    val realizedPnl: BigDecimal)
{
    fun isLong() = side == PositionSide.LONG
    fun isShort() = side == PositionSide.SHORT
    fun isOpen() = status == PositionStatus.OPEN
}

interface OrderService {
    fun createOrder(order: CreateOrder):Result<OrderUpdate>
    // TODO cancel/amend etc
}