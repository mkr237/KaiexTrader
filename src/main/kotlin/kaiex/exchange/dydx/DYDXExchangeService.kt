package kaiex.exchange.dydx

import kaiex.exchange.ExchangeService
import kaiex.model.*
import kaiex.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class DYDXExchangeService: KoinComponent, ExchangeService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    /**
     * Markets Service
     */
    override suspend fun subscribeMarketInfo(): Flow<MarketInfo> {

        val service : DYDXMarketsSocketEndpoint by inject()

        // connect to service
        log.info("Creating websocket for markets information")
        when(service.connect()) {
            is Resource.Success -> {

                // subscribe to symbol
                log.info("Subscribing market info updates")
                return when (service.subscribe()) {
                    is Resource.Success -> {

                        // observe updates
                        log.info("Listening for market info updates")
                        service.observeUpdates()
                    }
                    is Resource.Error -> {
                        log.error("Failed to subscribe to market info updates")
                        flow {  }
                    }
                }
            }
            is Resource.Error -> {
                log.error("Failed to create websocket for market info updates")
                return flow {  }
            }
        }
    }

    /**
     * Market Data Service
     */

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {

        val service : DYDXTradeSocketEndpoint by inject { parametersOf (symbol) }

        // connect to service
        log.info("Creating websocket for $symbol")
        when(service.connect()) {
            is Resource.Success -> {

                // subscribe to symbol
                log.info("Subscribing to $symbol")
                return when (service.subscribe()) {
                    is Resource.Success -> {

                        // observe updates
                        log.info("Listening for updates for $symbol")
                        service.observeUpdates()
                    }
                    is Resource.Error -> {
                        log.error("Failed to subscribe to $symbol")
                        flow {  }
                    }
                }
            }
            is Resource.Error -> {
                log.error("Failed to create websocket for $symbol")
                return flow {  }
            }
        }
    }

    override suspend fun subscribeCandles(symbol: String): Flow<Candle> {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook> {

        val service : DYDXOrderBookSocketEndpoint by inject { parametersOf (symbol) }

        // connect to service
        log.info("Creating websocket for $symbol")
        when(service.connect()) {
            is Resource.Success -> {

                // subscribe to symbol
                log.info("Subscribing to $symbol")
                return when (service.subscribe()) {
                    is Resource.Success -> {

                        // observe updates
                        log.info("Listening for updates for $symbol")
                        service.observeUpdates()
                    }
                    is Resource.Error -> {
                        log.error("Failed to subscribe to $symbol")
                        flow {  }
                    }
                }
            }
            is Resource.Error -> {
                log.error("Failed to create websocket for $symbol")
                return flow {  }
            }
        }
    }

    /**
     * Order Service
     */
    override fun createOrder(order: CreateOrder):Result<String> {

        val service : DYDXOrderEndpoint by inject()

        log.info("Creating order: $order")
        return runBlocking { service.post(order) }
    }

    /**
     * Account Service
     */
    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {

        val service : DYDXAccountSocketEndpoint by inject()

        // connect to service
        log.info("Creating websocket for account updates")
        when(service.connect()) {
            is Resource.Success -> {

                // subscribe to symbol
                log.info("Subscribing to account updates")
                return when (service.subscribe()) {
                    is Resource.Success -> {

                        // observe updates
                        log.info("Listening for account updates")
                        service.observeUpdates()
                    }
                    is Resource.Error -> {
                        log.error("Failed to subscribe to account updates")
                        flow {  }
                    }
                }
            }
            is Resource.Error -> {
                log.error("Failed to create websocket for account updates")
                return flow {  }
            }
        }
    }
}