package kaiex.strategy

import kaiex.core.*
import kaiex.model.*
import kaiex.ui.*
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import toCandles
import java.time.Instant

abstract class Strategy(val strategyId: String, val symbols:List<String>, val parameters:Map<String, String>) : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    private val marketDataManager : MarketDataManager by inject()
    private val orderManager : OrderManager by inject()
    private val riskManager : RiskManager by inject()
    private val reportManager : ReportManager by inject()
    private val uiServer : UIServer by inject()

    private val orders : MutableMap<String, OrderUpdate> = mutableMapOf() // by OrderId
    private val fills : MutableMap<String, MutableList<OrderFill>> = mutableMapOf()  // by orderId
    private val positions : MutableMap<String, PositionTracker> = mutableMapOf()  // by symbol
    abstract val config : StrategyConfig

    init {
        symbols.forEach { positions[it] = PositionTracker() }
    }

    abstract fun onStart()
    abstract fun onUpdate()
    abstract fun onStop()

    suspend fun start() {
        // register with the UI server and start
        uiServer.register(config)
        coroutineScope {
            subscribeMarketDataInfo(symbols[0])  // TODO - what about other symbols?
            sendStrategySnapshot()
            onStart()
        }
    }

    protected fun subscribeMarketDataInfo(symbol: String) {

        CoroutineScope(Dispatchers.Default).launch {
            marketDataManager.subscribeMarketInfo(symbol).listenForEvents().collect { marketInfo ->
                if(marketInfo.indexPrice != null) {
                    log.info("Received new index price for ${marketInfo.symbol}: ${marketInfo.indexPrice}")
                    positions[symbol]!!.updatePrice(marketInfo.indexPrice)
                    sendStrategySnapshot()
                }
            }
        }
    }

    protected fun subscribeCandles(symbol: String, onCandle: (Candle) -> Unit) {

        CoroutineScope(Dispatchers.Default).launch {
            marketDataManager.subscribeTrades(symbol).listenForEvents().toCandles().collect { candle -> onCandle(candle) }
        }
    }

    protected fun subscribeOrderBook(symbol: String, onUpdate: (OrderBook) -> Unit) {

        CoroutineScope(Dispatchers.Default).launch {
            marketDataManager.subscribeOrderBook(symbol).listenForEvents().collect { update -> onUpdate(update) }
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

                launch {
                    // listen for order updates
                    orderManager.subscribeOrderUpdates(orderId).collect { update ->
                        log.info("Received update for order: $orderId: $update")
                        orders[update.orderId] = update
                        sendStrategySnapshot()
                    }
                }

                launch {
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

                        sendStrategySnapshot()
                    }
                }

            }.onFailure { e ->
                log.error("Failed to create order: $e")
            }
        }
    }

    protected fun getCurrentPosition(symbol: String): Float {
        return positions[symbol]?.positionSize?.toFloat() ?: 0f
    }

    private fun sendStrategySnapshot() {
        uiServer.send(
            StrategySnapshot(
                strategyId,
                javaClass.simpleName,
                symbols,
                parameters,
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
            ))
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

    protected fun sendStrategyMarketDataUpdate(timestamp: Long, updates: List<SeriesUpdate>) {
        uiServer.send(StrategyMarketDataUpdate(strategyId, timestamp, updates))
    }
}
