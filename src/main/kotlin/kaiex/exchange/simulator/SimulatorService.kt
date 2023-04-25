package kaiex.exchange.simulator

import com.fersoft.types.Order
import kaiex.exchange.ExchangeService
import kaiex.exchange.simulator.adapters.BinanceAdapter
import kaiex.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue

class SimulatorService: KoinComponent, ExchangeService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeName = "SIM"
    private var lastTradePrice = 0f
    private val orderUpdates = ArrayBlockingQueue<OrderUpdate>(1)
    private val orderFills = ArrayBlockingQueue<OrderFill>(1)

    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {
        return flow {
            while(true) {
                val orderUpdate = orderUpdates.take()
                val orderFill = orderFills.take()
                emit(AccountUpdate(accountId, listOf(orderUpdate), listOf(orderFill), listOf()))
            }
        }
    }

    override suspend fun unsubscribeAccountUpdate() {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeMarketInfo(): Flow<MarketInfo> {
        return flow {
            while(true) {
                delay(5000)
                emit(MarketInfo("BTC-USD", MarketStatus.ONLINE, lastTradePrice, lastTradePrice))
            }
        }
    }

    override suspend fun unsubscribeMarketInfo() {
        log.info("unsubscribeMarketInfo")
    }

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {
        val dataFile = "Binance BTCUSDT-trades-2023-04-04.csv"
        log.info("Subscribing to: $dataFile")
        return TickPlayer(dataFile, BinanceAdapter(symbol), relativeTime = false).start().onEach {
            lastTradePrice = it.price
        }
    }

    override suspend fun unsubscribeTrades(symbol: String) {
        log.info("unsubscribeTrades")
    }

    override suspend fun subscribeCandles(symbol: String): Flow<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun unsubscribeCandles(symbol: String) {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook> {
        TODO("Not yet implemented")
    }

    override suspend fun unsubscribeOrderBook(symbol: String) {
        log.info("unsubscribeOrderBook")
    }

    override suspend fun createOrder(order: CreateOrder): Result<String> {

        val fillPrice = lastTradePrice
        val update = OrderUpdate(
            order.orderId,
            "exchangeName",
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
            fillPrice,
            order.size,
            0f,
            OrderRole.TAKER,
            order.createdAt,
            Instant.now().epochSecond)

        orderUpdates.put(update)
        orderFills.put(fill)
        return Result.success(order.orderId)
    }
}