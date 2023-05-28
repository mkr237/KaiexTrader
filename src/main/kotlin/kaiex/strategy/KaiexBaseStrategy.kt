package kaiex.strategy

import kaiex.model.MarketDataManager
import kaiex.model.TradeManager
import kaiex.model.ReportManager
import kaiex.indicator.Indicator
import kaiex.model.*
import kaiex.ui.Chart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal

/**
 * The base class for all strategies. Performs lifecycle activities, subscribes
 * to required market data and provides various functions for order management
 */
abstract class KaiexBaseStrategy : KoinComponent, KaiexStrategy {

    protected val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val marketDataManager : MarketDataManager by inject()
    private val tradeManager : TradeManager by inject()
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
                tradeManager.startAndSubscribe(this)
                    .takeWhile { update -> update.orders.isNotEmpty() || update.fills.isNotEmpty() || update.positions.isNotEmpty() }
                    .collect { update -> update.orders.forEach { onOrderUpdate(it) } }
            }
        }
    }

    override suspend fun stop() {
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
    protected fun buyAtMarket(symbol: String, size: Double) =
        createOrder(symbol, OrderType.MARKET, OrderSide.BUY, BigDecimal(35000), size.toBigDecimal(), BigDecimal(0.015), OrderTimeInForce.FOK)

    protected fun sellAtMarket(symbol: String, size: Double) =
        createOrder(symbol, OrderType.MARKET, OrderSide.SELL, BigDecimal(25000), size.toBigDecimal(), BigDecimal(0.015), OrderTimeInForce.FOK)

    protected fun setPosition(symbol: String, target: Double) {

        val position =  tradeManager.currentPosition(symbol)
        val potentialPosition = tradeManager.potentialPosition(symbol)

        if (potentialPosition.compareTo(position) != 0) {
            log.info("Position mismatch ($potentialPosition != $position) - cannot send order")
            return
        }

        val orderSize = target.toBigDecimal() - position
        if (orderSize > BigDecimal.ZERO)
            createOrder(
                symbol,
                OrderType.MARKET,
                OrderSide.BUY,
                BigDecimal(35000), // TODO calculate from index price or something
                orderSize.abs(),
                BigDecimal(0.015),
                OrderTimeInForce.FOK
            )
        else if (orderSize < BigDecimal.ZERO)
            createOrder(
                symbol,
                OrderType.MARKET,
                OrderSide.SELL,
                BigDecimal(25000),
                orderSize.abs(),
                BigDecimal(0.015),
                OrderTimeInForce.FOK
            )
    }

    private fun createOrder(symbol: String,
                            type: OrderType,
                            side: OrderSide,
                            price: BigDecimal,
                            size: BigDecimal,
                            limitFee: BigDecimal,
                            timeInForce: OrderTimeInForce) {

        tradeManager.createOrder(symbol, type, side, price, size, limitFee, timeInForce)
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
    protected fun getCurrentPosition(symbol: String) = tradeManager.currentPosition(symbol).toDouble()
    protected fun isFlat(symbol: String) = tradeManager.currentPosition(symbol) == BigDecimal.ZERO
}