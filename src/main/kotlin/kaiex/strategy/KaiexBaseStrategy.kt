package kaiex.strategy

import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.core.ReportManager
import kaiex.indicator.Indicator
import kaiex.model.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.math.absoluteValue

/**
 * The base class for all strategies. Performs lifecycle activities, subscribes
 * to required market data and provides various functions for order management
 */
abstract class KaiexBaseStrategy : KoinComponent, KaiexStrategy {

    protected val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val marketDataManager : MarketDataManager by inject()
    private val orderManager : OrderManager by inject()
    protected val reportManager : ReportManager by inject()

    private val orders : MutableMap<String, OrderUpdate> = mutableMapOf() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = mutableMapOf()  // by orderId
    private val positions : MutableMap<String, PositionTracker> = mutableMapOf()  // by symbol
    private val marketDataSnapshots:MutableMap<String, MarketDataSnapshot> = mutableMapOf()

    private val orderUpdatesScope = CoroutineScope(Dispatchers.Default)
    private val marketDataUpdatesScope = CoroutineScope(Dispatchers.Default)

    // TMP
    private val symbols = mutableListOf<String>()

    /**
     * Lifecycle functions
     */
    override suspend fun onCreate() {
        log.info("onCreate()")
        onStrategyCreate()
    }

    override suspend fun onMarketData(timestamp: Instant, snapshot: Map<String, MarketDataSnapshot>) {
        log.info("onMarketData() - $timestamp")
        onStrategyMarketData(marketDataSnapshots)
    }

    override suspend fun onOrderUpdate(update: OrderUpdate) {
        log.info("onOrderUpdate($update)")
        onStrategyOrderUpdate(update)
    }

    override suspend fun onDestroy() = runBlocking {
        log.info("onDestroy()")
        onStrategyDestroy()
    }

    abstract fun onStrategyCreate()
    abstract fun onStrategyMarketData(snapshot: Map<String, MarketDataSnapshot>)
    abstract fun onStrategyOrderUpdate(update: OrderUpdate)
    abstract fun onStrategyDestroy()

    /**
     * Market data functions
     */
    private suspend fun subscribeMarketDataInfo(symbol: String) {
        marketDataManager.subscribeMarketInfo(symbol).listenForEvents().collect { marketInfo ->
            if(marketInfo.indexPrice != null) {
                log.debug("Received market info: $marketInfo")
                positions[symbol]!!.updatePrice(marketInfo.indexPrice)
            }

            marketDataSnapshots[symbol]?.marketInfo = marketInfo
            onMarketData(marketInfo.createdAt, marketDataSnapshots)
        }
    }

    private suspend fun subscribeTrades(symbol: String) {
        marketDataManager.subscribeTrades(symbol).listenForEvents().collect { trade ->
            log.debug("Received trade: $trade")
        }
    }

    private suspend fun subscribeCandles(symbol: String) {
        marketDataManager.subscribeCandles(symbol).listenForEvents().collect { candle ->
            log.info("Received candle: $candle")

            // if the candle is complete, update any registered indicators and call strategy
            if(candle.complete) {
                marketDataSnapshots[symbol]?.lastCandle = candle
                marketDataSnapshots[symbol]?.indicators?.forEach { (_, indicator) ->
                    indicator.update(candle.close.toDouble())
                }
                onMarketData(Instant.ofEpochSecond(candle.startTimestamp), marketDataSnapshots)
            }
        }
    }

    protected suspend fun subscribeOrderBook(symbol: String) {
        marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { orderBook ->
            log.debug("Received order book: $orderBook")
            val bestBid = orderBook.bids[0].price.toDouble()
            val bestAsk = orderBook.asks[0].price.toDouble()
        }
    }

    protected fun addSymbol(symbol: String) {
        symbols.add(symbol)
        marketDataSnapshots[symbol] = MarketDataSnapshot()
        positions[symbol] = PositionTracker()

        // subscribe to market data
        marketDataUpdatesScope.launch { subscribeMarketDataInfo(symbol) }
        marketDataUpdatesScope.launch { subscribeCandles(symbol) }
        //launch { subscribeOrderBook(symbol) }
        //launch { subscribeTrades(symbol) }`
    }

    protected fun addIndicator(name: String, symbol: String, indicator: Indicator) {
        marketDataSnapshots[symbol]?.indicators?.putIfAbsent(name, indicator)
    }

    /**
     * Order management functions
     */
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
                    orders[update.orderId] = update

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

                    if(!symbols.contains(fill.symbol)) {
                        throw StrategyException("Received a fill for a unknown symbol: ${fill.symbol}")
                    }

                    log.info("Received fill for order: $orderId: $fill")
                    if(!fills.containsKey(fill.orderId))
                        fills[fill.orderId] = mutableListOf()
                    fills[fill.orderId]?.add(fill)
                    positions[symbol]!!.addTrade(fill)
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
        return positions[symbol]?.positionSize?.toFloat() ?: 0f
    }
}