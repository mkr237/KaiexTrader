package kaiex.exchange.simulator

import kaiex.exchange.ExchangeException
import kaiex.exchange.ExchangeService
import kaiex.exchange.simulator.adapters.BinanceAdapter
import kaiex.exchange.simulator.adapters.DYDXAdapter
import kaiex.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

class SimulatorService: KoinComponent, ExchangeService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeName = "SIM"
    private var lastTrade:Trade? = null

    private val positionBySymbol:MutableMap<String, PositionTracker> = mutableMapOf()
    private val updateQueue = ArrayBlockingQueue<AccountUpdate>(1)

    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {
        return flow {
            while(true) {
                val update = updateQueue.take()
                emit(update)
                if(update.orders.isEmpty() && update.fills.isEmpty() && update.positions.isEmpty()) break
            }
        }
    }

    override suspend fun subscribeMarketInfo(): Flow<MarketInfo> {
        return flow {
            while(true) {
                if(lastTrade != null) {

                    // update the index price of relevant position tracker(s)
                    if(!positionBySymbol.containsKey("BTC-USD")) {
                        //positionBySymbol["BTC-USD"]?.updatePrice(lastTrade!!.price)
                    }

                    // send the update
                    emit(
                        MarketInfo(
                            symbol = "BTC-USD",
                            status = MarketStatus.ONLINE,
                            indexPrice = lastTrade?.price!!,
                            oraclePrice = lastTrade?.price!!,
                            baseAsset = "BTC",
                            quoteAsset = "USD",
                            stepSize = BigDecimal.ZERO,
                            tickSize = BigDecimal.ZERO,
                            priceChange24H = BigDecimal.ZERO,
                            nextFundingRate = BigDecimal.ZERO,
                            nextFundingAt = Instant.now() + Duration.ofHours(1),
                            minOrderSize = BigDecimal.ZERO,
                            type = MarketType.PERPETUAL,
                            initialMarginFraction = BigDecimal.ZERO,
                            maintenanceMarginFraction = BigDecimal.ZERO,
                            transferMarginFraction = BigDecimal.ZERO,
                            volume24H = BigDecimal.ZERO,
                            trades24H = 0,
                            openInterest = BigDecimal.ZERO,
                            incrementalInitialMarginFraction = BigDecimal.ZERO,
                            incrementalPositionSize = BigDecimal.ZERO,
                            maxPositionSize = BigDecimal.ZERO,
                            baselinePositionSize = BigDecimal.ZERO,
                            assetResolution = BigInteger.ZERO,
                            syntheticAssetId = ""
                        )
                    )

                    // the strategy only needs one so we can end the flow
                    log.info("Ending market information updates")
                    break
                }
            }
        }
    }

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {
        val dataFile = "BTC-USD_20230526.dat"
        log.info("Subscribing to: $dataFile")
        return TickPlayer(dataFile, DYDXAdapter(symbol), relativeTime = false).start()
            .onCompletion { updateQueue.put(AccountUpdate("", emptyList(), emptyList(), emptyList())) }
            .onEach { lastTrade = it }
    }

    override suspend fun subscribeCandles(symbol: String): Flow<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook> {
        TODO("Not yet implemented")
    }

    override fun createOrder(order: CreateOrder): Result<OrderUpdate> {

        val fillPrice = lastTrade?.price
        val update = OrderUpdate(
            order.orderId,
            exchangeName,
            "0",
            order.symbol,
            order.type,
            order.side,
            order.price,
            order.size,
            BigDecimal.ZERO,
            OrderStatus.FILLED,
            order.timeInForce,
            order.createdAt,
            order.createdAt + Duration.ofHours(1))

        val fill = OrderFill(
            UUID.randomUUID().toString(),
            order.orderId,
            order.symbol,
            order.type,
            order.side,
            fillPrice!!,
            order.size,
            BigDecimal.ZERO,
            OrderRole.TAKER,
            order.createdAt,
            Instant.now())

        val positionTracker = positionBySymbol.computeIfAbsent(order.symbol) { PositionTracker(order.symbol) }
        positionTracker.addTrade(fill)
        updateQueue.put(AccountUpdate(UUID.randomUUID().toString(), listOf(update), listOf(fill), positionTracker.positionList))
        return Result.success(update)
    }
}