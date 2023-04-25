package kaiex.strategy

import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kaiex.ui.*
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.time.Instant
import java.util.concurrent.Flow

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
    private val uiServer : UIServer by inject()

    private val orders : MutableMap<String, OrderUpdate> = mutableMapOf() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = mutableMapOf()  // by orderId
    private val positions : MutableMap<String, PositionTracker> = mutableMapOf()  // by symbol
    protected var config:StrategyConfig = StrategyConfig()

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

    override fun onMarketData() {
        log.info("onMarketData()")
        onStrategyMarketData()
    }

    override fun onOrderUpdate(update: OrderUpdate) {
        log.info("onOrderUpdate($update)")
        onStrategyOrderUpdate(update)
    }

    override suspend fun onDestroy() = runBlocking {
        log.info("onDestroy()")
        onStrategyDestroy()
    }

    abstract suspend fun onStrategyCreate()
    abstract fun onStrategyMarketData()
    abstract fun onStrategyOrderUpdate(update: OrderUpdate)
    abstract suspend fun onStrategyDestroy()

    /**
     * Market data functions
     */
    private suspend fun subscribeMarketDataInfo(symbol: String) {
        marketDataManager.subscribeMarketInfo(symbol).listenForEvents().collect { marketInfo ->
            if(marketInfo.indexPrice != null) {
                log.info("Received market info: $marketInfo")
                positions[symbol]!!.updatePrice(marketInfo.indexPrice)
                sendStrategySnapshot()
            }
        }
    }

    private suspend fun subscribeCandles(symbol: String) {
        marketDataManager.subscribeTrades(symbol).listenForEvents().toCandles().collect { candle ->
            log.info("Received candle: $candle")
            val updates = listOf(SeriesUpdate.CandleUpdate(
                "price",
                candle.open.toDouble(),
                candle.high.toDouble(),
                candle.low.toDouble(),
                candle.close.toDouble())
            )
            uiServer.send(StrategyMarketDataUpdate(config.strategyId, candle.startTimestamp, updates))
        }
    }

    protected suspend fun subscribeOrderBook(symbol: String) {
        marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { update ->
            log.info("Received order book update: $update")
            val bestBid = update.bids[0].price.toDouble()
            val bestAsk = update.asks[0].price.toDouble()
            //val midPrice = bestBid + ((bestAsk - bestBid) / 2)
            val updates = listOf(SeriesUpdate.NumericUpdate("best-bid", bestBid), SeriesUpdate.NumericUpdate("best-ask", bestAsk))
            uiServer.send(StrategyMarketDataUpdate(config.strategyId, update.receivedAt.epochSecond, updates))
        }
    }

    /**
     * Order management functions
     */
    protected suspend fun buyAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.BUY, 35000f, size, 0.015f, OrderTimeInForce.FOK)
    protected suspend fun sellAtMarket(symbol: String, size: Float) =
        createOrder(symbol, OrderType.MARKET, OrderSide.SELL, 25000f, size, 0.015f, OrderTimeInForce.FOK)
    private suspend fun createOrder(symbol: String,
                                    type: OrderType,
                                    side: OrderSide,
                                    price: Float,
                                    size: Float,
                                    limitFee: Float,
                                    timeInForce: OrderTimeInForce
    ) {

        coroutineScope {
            orderManager.createOrder(symbol, type, side, price, size, limitFee, timeInForce).onSuccess { orderId ->
                log.info("Successfully created order: $orderId")

                launch {
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
                launch {
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
    }

    /**
     * Misc. functions
     */
    protected fun getCurrentPosition(symbol: String): Float {
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
                86.2f,
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