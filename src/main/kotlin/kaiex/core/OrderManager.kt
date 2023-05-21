package kaiex.core

import kaiex.exchange.ExchangeService
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class OrderManager : KoinComponent {

    private val exchange = "DYDX" // only support DYDX for now

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService: ExchangeService by inject()

    private val orders : MutableMap<String, OrderUpdate> = ConcurrentHashMap() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = ConcurrentHashMap()  // by orderId
    private val positions : MutableMap<String, Position> = ConcurrentHashMap()  // by symbol
    private val potentialPositions : MutableMap<String, Float> = ConcurrentHashMap()  // by symbol

    private val listener = MutableSharedFlow<AccountUpdate>()

    val orderMap : Map<String, OrderUpdate>
        get() = orders.toMap()
    val fillMap : Map<String, MutableList<OrderFill>>
        get() = fills.toMap()
    val positionMap : Map<String, Position>
        get() = positions.toMap()

    suspend fun startAndSubscribe(scope: CoroutineScope): SharedFlow<AccountUpdate> {
        scope.launch { trackAccountUpdates("0") }
        return listener.asSharedFlow()
    }

    private suspend fun trackAccountUpdates(accountId: String) {
        log.info("Subscribing to account updates")
        exchangeService.subscribeAccountUpdates(accountId)
            .collect { accountUpdate: AccountUpdate ->
                handleAccountUpdate(accountUpdate)
                listener.emit(accountUpdate)
            }
    }

    private fun handleAccountUpdate(accountUpdate: AccountUpdate) {
        log.info("Received Account Update: $accountUpdate")

        // handle order updates
        accountUpdate.orders.forEach { update ->
            orders[update.orderId] = update
            // TODO if order has been cancelled, we need to reduce potential position
        }

        // handle fills
        accountUpdate.fills.forEach { fill ->
            fills.getOrPut(fill.orderId) { mutableListOf() }.add(fill)
        }

        // handle position updates
        accountUpdate.positions.forEach { position ->
            positions[position.symbol] = position
        }
    }

    fun createOrder(symbol: String,
                    type: OrderType,
                    side: OrderSide,
                    price: Float,
                    size: Float,
                    limitFee: Float,
                    timeInForce: OrderTimeInForce
    ): Result<OrderUpdate> {

        val order = CreateOrder(
            UUID.randomUUID().toString(),
            exchange,
            symbol,
            type,
            side,
            price,
            size,
            limitFee,
            timeInForce,
            false,
            false,
            Instant.now().epochSecond
        )

        val signedSize = if(side == OrderSide.BUY) size else size * -1
        potentialPositions[symbol] = potentialPositions.getOrDefault(symbol, 0f) + signedSize

        log.info("Creating new order: $order")
        val result = exchangeService.createOrder(order)
        result.onSuccess {
            orders[it.orderId] = it
            log.info("Update: $it")}
        result.onFailure {
            // TODO remove update from set
        }
        return result
    }

    // TODO cancel/amend etc

    fun currentPosition(symbol: String) = positions[symbol]?.size ?: 0f
    fun potentialPosition(symbol: String) = potentialPositions.getOrDefault(symbol, 0f)
    fun hasPendingOrders(symbol: String) = orders.values.any { order -> order.symbol == symbol && order.status == OrderStatus.PENDING }
}