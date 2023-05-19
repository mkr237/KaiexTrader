package kaiex.exchange.simulator

import kaiex.exchange.ExchangeService
import kaiex.exchange.simulator.adapters.BinanceAdapter
import kaiex.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    private val updateQueue = ArrayBlockingQueue<OrderUpdate>(1)
    private val fillQueue = ArrayBlockingQueue<OrderFill>(1)
    private val positionQueue = ArrayBlockingQueue<Position>(1)

    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {
        return flow {
            while(true) {
                val orderUpdate = updateQueue.take()
                val orderFill = fillQueue.take()
                val positionUpdate = positionQueue.take()
                emit(AccountUpdate(accountId, listOf(orderUpdate), listOf(orderFill), listOf(positionUpdate)))
            }
        }
    }

    override suspend fun subscribeMarketInfo(): Flow<MarketInfo> {
        return flow {
            while(true) {
                if(lastTrade != null) {

                    // update the index price of relevant position tracker(s)
                    if(!positionBySymbol.containsKey("BTC-USD")) {
                        positionBySymbol["BTC-USD"]?.updatePrice(lastTrade!!.price)
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
                    throw CancellationException("Market information sent")
                }
            }
        }
    }

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {
        val dataFile = "Binance BTCUSDT-trades-2023-04-04.csv"
        log.info("Subscribing to: $dataFile")
        return TickPlayer(dataFile, BinanceAdapter(symbol), relativeTime = false).start().onEach {
            lastTrade = it
        }
    }

    override suspend fun subscribeCandles(symbol: String): Flow<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook> {
        TODO("Not yet implemented")
    }

    override fun createOrder(order: CreateOrder): Result<String> {

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
            order.createdAt + 1000L)

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
            Instant.now().epochSecond)

        if(!positionBySymbol.containsKey(fill.symbol)) {
            positionBySymbol[fill.symbol] = PositionTracker()
        }

        val p = positionBySymbol[fill.symbol]!!
        p.addTrade(fill)

        val position = Position(
            UUID.randomUUID().toString(),
            fill.symbol,
            if(p.positionSize > BigDecimal.ZERO) PositionSide.LONG else PositionSide.SHORT,
            p.avgEntryPrice.toFloat(),
            p.avgExitPrice.toFloat(),
            p.positionSize.toFloat(),
            p.unrealizedPnl.toFloat(),
            Instant.now().epochSecond,  // TODO pull from tracker
            Instant.now().epochSecond)  // // TODO pull from tracker

        updateQueue.put(update)
        fillQueue.put(fill)
        positionQueue.put(position)
        return Result.success(order.orderId)
    }
}