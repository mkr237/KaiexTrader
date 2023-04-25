package kaiex.strategy

import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.model.*
import kaiex.ui.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
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
    //private val riskManager : RiskManager by inject()
    //private val reportManager : ReportManager by inject()
    protected val uiServer : UIServer by inject()

    private val orders : MutableMap<String, OrderUpdate> = mutableMapOf() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = mutableMapOf()  // by orderId
    private val positions : MutableMap<String, PositionTracker> = mutableMapOf()  // by symbol
    protected var config:StrategyConfig = StrategyConfig()

    private val marketDataSnapshot = MarketDataSnapshot()

    private val orderUpdatesScope = CoroutineScope(Dispatchers.Default)

    /**
     * Lifecycle functions
     */
    override suspend fun onCreate(config: StrategyConfig) = runBlocking {
        log.info("onCreate() with config: $config")
        this@KaiexBaseStrategy.config = config
        config.symbols.forEach { positions[it] = PositionTracker() }

        uiServer.register(config)
        log.info("Base strategy onCreate() finished")
        config.symbols.forEach { symbol ->
            launch { subscribeMarketDataInfo(symbol) }
            launch { subscribeCandles(symbol) }
            //launch { subscribeOrderBook(symbol) }
        }

        sendStrategySnapshot()
        onStrategyCreate()
    }

    override suspend fun onMarketData() {
        log.info("onMarketData()")
        onStrategyMarketData(marketDataSnapshot)
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
    abstract fun onStrategyMarketData(snapshot: MarketDataSnapshot)
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
                sendStrategySnapshot()
            }

            marketDataSnapshot.updateMarketInfo(marketInfo)
            onStrategyMarketData(marketDataSnapshot)
        }
    }

    private suspend fun subscribeTrades(symbol: String) {
        marketDataManager.subscribeTrades(symbol).listenForEvents().collect { trade ->
            log.debug("Received trade: $trade")
            // TODO
            //uiServer.send(StrategyMarketDataUpdate(config.strategyId, candle.startTimestamp, updates))
        }
    }

    private suspend fun subscribeCandles(symbol: String) {
        marketDataManager.subscribeCandles(symbol).listenForEvents().collect { candle ->
            log.info("Received candle: $candle")
            val updates = listOf(SeriesUpdate.CandleUpdate(
                "price",
                candle.open.toDouble(),
                candle.high.toDouble(),
                candle.low.toDouble(),
                candle.close.toDouble())
            )

            marketDataSnapshot.updateCandle(candle)
            onStrategyMarketData(marketDataSnapshot)

            uiServer.send(StrategyMarketDataUpdate(config.strategyId, candle.startTimestamp, updates))
        }
    }

    protected suspend fun subscribeOrderBook(symbol: String) {
        marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { orderBook ->
            log.debug("Received order book: $orderBook")
            val bestBid = orderBook.bids[0].price.toDouble()
            val bestAsk = orderBook.asks[0].price.toDouble()
            //val midPrice = bestBid + ((bestAsk - bestBid) / 2)
            val updates = listOf(SeriesUpdate.NumericUpdate("best-bid", bestBid), SeriesUpdate.NumericUpdate("best-ask", bestAsk))
            uiServer.send(StrategyMarketDataUpdate(config.strategyId, orderBook.receivedAt.epochSecond, updates))
        }
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
                    sendStrategySnapshot()

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

                    if(!config.symbols.contains(fill.symbol)) {
                        throw StrategyException("Received a fill for a unknown symbol: ${fill.symbol}")
                    }

                    log.info("Received fill for order: $orderId: $fill")
                    if(!fills.containsKey(fill.orderId))
                        fills[fill.orderId] = mutableListOf()
                    fills[fill.orderId]?.add(fill)

                    positions[symbol]!!.addTrade(fill)

                    sendStrategySnapshot()
                }
            }

        }.onFailure { e ->
            log.error("Failed to create order: $e")
        }
    }

    /**
     * Misc. functions
     */
    private fun getCurrentPosition(symbol: String): Float {
        return positions[symbol]?.positionSize?.toFloat() ?: 0f
    }

    private fun sendStrategySnapshot() {
        uiServer.send(
            StrategySnapshot(
                config.strategyId,
                javaClass.simpleName,
                config.symbols,
                config.parameters,
                Instant.now().epochSecond,
                23.2f,
                23,
                54f,
                22f,
                1.31f,
                emptyMap(),
                orders,
                extractFills(),
                extractPositions()
            )
        )
    }

    private fun extractFills(): Map<String, OrderFill> {
        return fills
            .flatMap { (_, fills) ->
                fills.map { fill ->
                    fill.fillId to fill
                }
            }.toMap()
    }

    private fun extractPositions(): Map<String, StrategyPosition> {
        val positionMap = mutableMapOf<String, StrategyPosition>()
        positions.map { (symbol, position) ->
            positionMap[symbol] = StrategyPosition(
                symbol,
                position.positionSize.toFloat(),
                position.avgEntryPrice.toFloat(),
                position.avgExitPrice.toFloat(),
                position.realizedPnl.toFloat(),
                position.unrealizedPnl.toFloat(),
                position.marketPrice?.toFloat() ?: 0f
            )
        }

        return positionMap
    }
}