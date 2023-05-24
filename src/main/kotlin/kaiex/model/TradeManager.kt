package kaiex.model

import kaiex.exchange.ExchangeService
import kaiex.api.Metrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TradeManager : KoinComponent {

    private val exchange = "DYDX" // only support DYDX for now

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService: ExchangeService by inject()

    private val orders : MutableMap<String, OrderUpdate> = ConcurrentHashMap() // update by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = ConcurrentHashMap()  // list of fills by orderId
    private val positions : MutableMap<String, MutableMap<String, Position>> = ConcurrentHashMap()  // map of positionId to position by symbol
    private val potentialPositions : MutableMap<String, BigDecimal> = ConcurrentHashMap()  // potential position by symbol

    private val listener = MutableSharedFlow<AccountUpdate>()

    val orderMap : Map<String, OrderUpdate>
        get() = orders.toMap()
    val fillMap : Map<String, MutableList<OrderFill>>
        get() = fills.toMap()
    val positionMap : Map<String, MutableMap<String, Position>>
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

        accountUpdate.orders.forEach { update ->
            orders[update.orderId] = update
            // TODO if order has been cancelled, we need to reduce potential position
        }

        accountUpdate.fills.forEach { fill -> fills.getOrPut(fill.orderId) { mutableListOf() }.add(fill) }
        accountUpdate.positions.forEach { position ->
            val positionMap = positions.computeIfAbsent(position.symbol) { mutableMapOf() }
            positionMap[position.positionId] = position
        }
    }

    fun createOrder(symbol: String,
                    type: OrderType,
                    side: OrderSide,
                    price: BigDecimal,
                    size: BigDecimal,
                    limitFee: BigDecimal,
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
            Instant.now()
        )

        val signedSize = if(side == OrderSide.BUY) size else size * BigDecimal.ONE.negate()
        potentialPositions[symbol] = potentialPositions.getOrDefault(symbol, BigDecimal.ZERO) + signedSize

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

    fun currentPosition(symbol: String) = positions[symbol]?.values?.firstOrNull { it.status == PositionStatus.OPEN }?.let {
            when (it.side) {
                PositionSide.LONG -> it.size
                PositionSide.SHORT -> -it.size
            }
        } ?: BigDecimal.ZERO
    fun potentialPosition(symbol: String) = potentialPositions.getOrDefault(symbol, BigDecimal.ZERO)
    fun hasPendingOrders(symbol: String) = orders.values.any { order -> order.symbol == symbol && order.status == OrderStatus.PENDING }

    fun getTradeMetrics(): Metrics {  // TODO trade manager shouldn't know about UI stuff
        var pnl = BigDecimal.ZERO
        var numTrades = 0
        var numWins = 0

        positions.values.forEach { positionMap ->
            numTrades += positionMap.size
            positionMap.values.forEach { position ->
                pnl += position.realizedPnl
                if(position.realizedPnl > BigDecimal.ZERO) numWins++
            }
        }

        val winRate = if(numTrades > 0) numWins.toFloat() / numTrades else 0
        val sharpe = BigDecimal.ZERO

        return Metrics(pnl.toDouble(), numTrades, winRate.toDouble(), sharpe.toDouble())
    }
}