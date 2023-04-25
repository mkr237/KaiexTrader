package kaiex.exchange.simulator

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

class SimulatorService: KoinComponent, ExchangeService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var lastTradePrice = 0f

    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {
        return flow {
            AccountUpdate("0", listOf(), listOf(), listOf())
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
        TODO("Not yet implemented")
    }
}