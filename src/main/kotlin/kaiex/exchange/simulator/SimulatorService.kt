package kaiex.exchange.simulator

import kaiex.exchange.ExchangeService
import kaiex.exchange.simulator.adapters.BinanceAdapter
import kaiex.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
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
                            "BTC-USD",
                            MarketStatus.ONLINE,
                            lastTrade?.price!!,
                            lastTrade?.price!!,
                            lastTrade?.createdAt!!
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
        val dataFile = "Binance BTCUSDT-trades-2023-04-04.csv"
        log.info("Subscribing to: $dataFile")
        return TickPlayer(dataFile, BinanceAdapter(symbol), relativeTime = false).start()
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
            0f,
            OrderStatus.FILLED,
            order.timeInForce,
            order.createdAt,
            order.createdAt + 60000L)

        val fill = OrderFill(
            UUID.randomUUID().toString(),
            order.orderId,
            order.symbol,
            order.type,
            order.side,
            fillPrice!!,
            order.size,
            0f,
            OrderRole.TAKER,
            order.createdAt,
            Instant.now().toEpochMilli())

        val positionTracker = positionBySymbol.computeIfAbsent(order.symbol) { PositionTracker(order.symbol) }
        positionTracker.addTrade(fill)
        updateQueue.put(AccountUpdate(UUID.randomUUID().toString(), listOf(update), listOf(fill), positionTracker.positionList))
        return Result.success(update)
    }
}