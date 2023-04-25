package kaiex.core

import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class OrderManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val dydxExchangeService : DYDXExchangeService by inject()
    private val accountManager : AccountManager by inject()

    // Need these?
    private val orders : MutableMap<String, OrderUpdate> = mutableMapOf() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = mutableMapOf()  // by orderId
    private val positions : MutableMap<String, Position> = mutableMapOf()  // by symbol

    private val orderUpdateSubscriptions = mutableMapOf<String, MutableSharedFlow<OrderUpdate>>()
    private val orderFillSubscriptions = mutableMapOf<String, MutableSharedFlow<OrderFill>>()
    private val positionSubscriptions = mutableMapOf<String, MutableSharedFlow<Position>>()

    init {
        log.info("Starting")
        CoroutineScope(Dispatchers.Default).launch {
            launch {
                accountManager.subscribeAccountUpdates("0").listenForEvents().collect { update ->
                    handleAccountUpdate(update)
                }
            }
        }
    }

    fun start() {

    }

    private suspend fun handleAccountUpdate(accountUpdate: AccountUpdate) {
        log.info("Received Account Update: $accountUpdate")

        // handle order updates
        accountUpdate.orders.forEach { update ->

            // store the update
            orders[update.orderId] = update

            //
            if(!orderUpdateSubscriptions.containsKey(update.orderId))
                orderUpdateSubscriptions[update.orderId] = MutableSharedFlow()

            //
            orderUpdateSubscriptions[update.orderId]?.emit(update)
        }

        // handle fills
        accountUpdate.fills.forEach { fill ->

            // store the fill
            if(!fills.containsKey(fill.orderId))
                fills[fill.orderId] = mutableListOf()
            fills[fill.orderId]?.add(fill)

            //
            if(!orderFillSubscriptions.containsKey(fill.orderId))
                orderFillSubscriptions[fill.orderId] = MutableSharedFlow()

            //
            orderFillSubscriptions[fill.orderId]?.emit(fill)
        }

        // handle position updates
        accountUpdate.positions.forEach { position ->

            // store the position
            positions[position.symbol] = position

            //
            if(!positionSubscriptions.containsKey(position.symbol))
                positionSubscriptions[position.symbol] = MutableSharedFlow()

            //
            positionSubscriptions[position.symbol]?.emit(position)
        }
    }

    fun subscribeOrderUpdates(orderId: String): MutableSharedFlow<OrderUpdate> {
        if(!orderUpdateSubscriptions.containsKey(orderId))
            orderUpdateSubscriptions[orderId] = MutableSharedFlow()

        return orderUpdateSubscriptions[orderId]!!
    }

    fun subscribeOrderFills(orderId: String): MutableSharedFlow<OrderFill> {
        if(!orderFillSubscriptions.containsKey(orderId))
            orderFillSubscriptions[orderId] = MutableSharedFlow()

        return orderFillSubscriptions[orderId]!!
    }

    fun subscribePositions(symbol: String): MutableSharedFlow<Position> {
        if(!positionSubscriptions.containsKey(symbol))
            positionSubscriptions[symbol] = MutableSharedFlow()

        return positionSubscriptions[symbol]!!
    }

    suspend fun createOrder(symbol: String,
                            type: OrderType,
                            side: OrderSide,
                            price: Float,
                            size: Float,
                            limitFee: Float,
                            timeInForce: OrderTimeInForce): Result<String> {

        val order = CreateOrder(
            UUID.randomUUID().toString(),
            "DYDX",
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

        log.info("Creating new order: $order")
        return dydxExchangeService.createOrder(order)
    }
}