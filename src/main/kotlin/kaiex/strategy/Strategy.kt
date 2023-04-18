package kaiex.strategy

import kaiex.core.*
import kaiex.model.*
import kaiex.ui.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.time.Instant

abstract class Strategy(val strategyId: String) : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val accountManager : AccountManager by inject()

    private val marketDataManager : MarketDataManager by inject()
    private val orderManager : OrderManager by inject()
    private val riskManager : RiskManager by inject()
    private val reportManager : ReportManager by inject()
    private val uiServer : UIServer by inject()

    protected val orderIds : MutableSet<String> = mutableSetOf()
    protected val orders : MutableMap<String, OrderUpdate> = mutableMapOf()
    protected val fills : MutableMap<String, OrderFill> = mutableMapOf()
    protected val positions : MutableMap<String, Position> = mutableMapOf()
    abstract val config : StrategyChartConfig

    abstract fun onStart()
    abstract fun onUpdate()
    abstract fun onStop()

    suspend fun start() {

        // register with the UI server
        uiServer.register(config)

        //
        coroutineScope {
            handleAccountUpdate(AccountUpdate(strategyId, emptyList(), emptyList(), emptyList()))
            async {
                accountManager.subscribeAccountUpdates("0").listenForEvents().collect { update ->
                    handleAccountUpdate(update)
                    onUpdate()
                }
            }
            onStart()
        }
    }

    protected fun subscribeCandles(symbol: String,
                                   onCandle: (Candle) -> Unit) {

        CoroutineScope(Dispatchers.Default).launch {
            marketDataManager.subscribeTrades(symbol).listenForEvents().toCandles().collect { candle -> onCandle(candle) }
        }
    }

    protected fun createOrder(symbol: String,
                              type: OrderType,
                              side: OrderSide,
                              price: Float,
                              size: Float,
                              limitFee: Float,
                              timeInForce: OrderTimeInForce) {

        CoroutineScope(Dispatchers.Default).launch {
            orderManager.createOrder(symbol, type, side, price, size, limitFee, timeInForce).onSuccess { orderId ->
                log.info("Successfully created order: $orderId")
                orderIds.add(orderId)
            }.onFailure { e ->
                log.error("Failed to create order: $e")
            }
        }
    }

    private fun handleAccountUpdate(update: AccountUpdate) {
        log.info("Received Account Update: $update")
        update.orders.filter { order ->
            log.info("Checking order ${order.orderId}")
            orderIds.contains(order.orderId)
        }.forEach { order ->
            log.info("Updating order ${order.orderId}")
            orders[order.orderId] = order
        }
        update.fills.filter { fill -> orderIds.contains(fill.orderId) }.forEach { fill -> fills[fill.fillId] = fill }
        update.positions.forEach { position -> positions[position.positionId] = position } //TODO
        uiServer.send(
            StrategySnapshot(
                strategyId,
                Instant.now().epochSecond,
                86.2f,
                23,
                54f,
                22f,
                1.31f,
                emptyMap(),
                orders,
                fills,
                positions))
    }

    protected fun getCurrentPosition(symbol: String): Float {
        return 0f
    }

    protected fun sendStrategyMarketDataUpdate(timestamp: Long, updates: List<SeriesUpdate>) {
        uiServer.send(StrategyMarketDataUpdate(strategyId, timestamp, updates))
    }
}
