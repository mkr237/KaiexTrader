package kaiex.strategy

import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.core.ReportManager
import kaiex.indicator.Indicator
import kaiex.model.*
import kaiex.ui.Chart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

    private val marketDataManager : MarketDataManager by inject()
    private val orderManager : OrderManager by inject()
    private val reportManager : ReportManager by inject()

    /**
     * Lifecycle functions
     */
    override suspend fun start() {
        log.info("Starting strategy")
        onCreate()
        coroutineScope {
            launch {
                marketDataManager.startAndSubscribe(this)
                    .takeWhile { snapshot -> snapshot.isNotEmpty() }
                    .collect { snapshot -> onMarketData(snapshot) }
            }

            launch {
                orderManager.startAndSubscribe(this)
                    .takeWhile { update -> update.orders.isNotEmpty() || update.fills.isNotEmpty() || update.positions.isNotEmpty() }
                    .collect { update -> update.orders.forEach { onOrderUpdate(it) } }
            }
        }
    }

    override fun stop() = runBlocking {
        log.info("Stopping strategy")
        onDestroy()
    }

    abstract fun onCreate()
    abstract fun onMarketData(snapshot: Map<String, MarketDataSnapshot>)
    abstract fun onOrderUpdate(update: OrderUpdate)
    abstract fun onDestroy()

    /**
     * Market data functions
     */
    fun addSymbol(symbol: String) = marketDataManager.addSymbol(symbol)
    fun addIndicator(name:String, symbol: String, indicator: Indicator) = marketDataManager.addIndicator(name, symbol, indicator)
    fun addChart(chart: Chart) = reportManager.addChart(chart)

    /**
     * Order management functions
     */
    //TODO remove the hard-coded values
    protected fun buyAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.BUY, 35000f, size, 0.015f, OrderTimeInForce.FOK)

    protected fun sellAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.SELL, 25000f, size, 0.015f, OrderTimeInForce.FOK)

    protected fun setPosition(symbol: String, target: Float) {

        val position =  orderManager.currentPosition(symbol)
        val potentialPosition = orderManager.potentialPosition(symbol)

        if (potentialPosition != position) {
            log.info("Position mismatch - cannot send order")
            return
        }

        val orderSize = target - position
        if (orderSize > 0)
            createOrder(
                symbol,
                OrderType.MARKET,
                OrderSide.BUY,
                35000f, // TODO calculate from index price or something
                orderSize.absoluteValue,
                0.015f,
                OrderTimeInForce.FOK
            )
        else if (orderSize < 0)
            createOrder(
                symbol,
                OrderType.MARKET,
                OrderSide.SELL,
                25000f,
                orderSize.absoluteValue,
                0.015f,
                OrderTimeInForce.FOK
            )
    }

    private fun createOrder(symbol: String,
                            type: OrderType,
                            side: OrderSide,
                            price: Float,
                            size: Float,
                            limitFee: Float,
                            timeInForce: OrderTimeInForce) {

        orderManager.createOrder(symbol, type, side, price, size, limitFee, timeInForce)
            .onSuccess { update ->
                log.info("Successfully created order: ${update.orderId}")
            }
            .onFailure { e ->
                log.error("Failed to create order: $e")
            }
    }

    /**
     * Misc. functions
     */
    protected fun getCurrentPosition(symbol: String): Float = orderManager.currentPosition(symbol)
}