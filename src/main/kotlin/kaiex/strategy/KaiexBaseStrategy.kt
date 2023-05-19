package kaiex.strategy

import kaiex.core.OrderManager
import kaiex.core.ReportManager
import kaiex.indicator.Indicator
import kaiex.model.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.absoluteValue

/**
 * The base class for all strategies. Performs lifecycle activities, subscribes
 * to required market data and provides various functions for order management
 */
abstract class KaiexBaseStrategy : KoinComponent, KaiexStrategy {

    protected val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    protected val marketDataTracker = MarketDataTracker(::onMarketData)
    private val orderManager : OrderManager by inject()
    protected val reportManager : ReportManager by inject()

    private val orderUpdatesScope = CoroutineScope(Dispatchers.Default)

    private val jobs = mutableListOf<Job>()

    /**
     * Lifecycle functions
     */
    override suspend fun onCreate() = runBlocking {
        log.debug("onCreate()")
        jobs.add(marketDataTracker.trackMarketInfo())
        onStrategyCreate()
        jobs.joinAll()
    }

    override suspend fun onMarketData(snapshot: Map<String, MarketDataSnapshot>) {
        log.debug("onMarketData($snapshot)")
        onStrategyMarketData(snapshot)
    }

    override suspend fun onOrderUpdate(update: OrderUpdate) {
        log.debug("onOrderUpdate($update)")
        onStrategyOrderUpdate(update)
    }

    override suspend fun onDestroy() = runBlocking {
        log.debug("onDestroy()")
        onStrategyDestroy()
    }

    abstract fun onStrategyCreate()
    abstract fun onStrategyMarketData(snapshot: Map<String, MarketDataSnapshot>)
    abstract fun onStrategyOrderUpdate(update: OrderUpdate)
    abstract fun onStrategyDestroy()

    /**
     * Market data functions
     */
    fun addTrades(symbol: String) {
        jobs.add(marketDataTracker.trackTrades(symbol))
    }

    fun addIndicator(name:String, symbol: String, indicator: Indicator) {
        marketDataTracker.addIndicator(name, symbol, indicator)
    }

    /**
     * Order management functions
     */
    //TODO remove the hard-coded values
    protected fun buyAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.BUY, 35000f, size, 0.015f, OrderTimeInForce.FOK)

    protected fun sellAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.SELL, 25000f, size, 0.015f, OrderTimeInForce.FOK)

    protected fun setPosition(symbol: String, target: Float) {
        val orderSize = target - getCurrentPosition(symbol)
        if(orderSize > 0)
            createOrder(symbol, OrderType.MARKET, OrderSide.BUY, 35000f, orderSize.absoluteValue, 0.015f, OrderTimeInForce.FOK)
        else if(orderSize < 0)
            createOrder(symbol, OrderType.MARKET, OrderSide.SELL, 25000f, orderSize.absoluteValue, 0.015f, OrderTimeInForce.FOK)
    }

    private fun createOrder(symbol: String,
                                    type: OrderType,
                                    side: OrderSide,
                                    price: Float,
                                    size: Float,
                                    limitFee: Float,
                                    timeInForce: OrderTimeInForce) {

        orderManager.createOrder(symbol, type, side, price, size, limitFee, timeInForce).onSuccess { orderId ->

            log.info("Successfully created order: $orderId")

            orderUpdatesScope.launch {
                // listen for order updates
                orderManager.subscribeOrderUpdates(orderId).collect { update ->
                    log.info("Received update for order: $orderId: $update")
                    if(update.status == OrderStatus.FILLED) {
                        log.info("Order ${update.orderId} is FILLED")
                        throw CancellationException("Order ${update.orderId} is FILLED")
                    }
                }
            }

            // TODO this coroutine will never end
            orderUpdatesScope.launch {
                // listen for order fills
                orderManager.subscribeOrderFills(orderId).collect { fill ->
                    log.info("Received fill for order: $orderId: $fill")
//                    if(!symbols.contains(fill.symbol)) {
//                        throw StrategyException("Received a fill for a unknown symbol: ${fill.symbol}")
//                    }
                }
            }

        }.onFailure { e ->
            log.error("Failed to create order: $e")
        }
    }

    /**
     * Misc. functions
     */
    protected fun getCurrentPosition(symbol: String): Float {
        return orderManager.positionMap[symbol]?.size ?: 0f
    }
}