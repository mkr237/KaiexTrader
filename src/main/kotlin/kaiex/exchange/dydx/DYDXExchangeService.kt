package kaiex.exchange.dydx

import kaiex.model.*
import kaiex.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DYDXExchangeService: KoinComponent, MarketDataService, OrderService, AccountService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    /**
     * Market Data Service
     */

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {

        val service : DYDXTradeStream by inject { parametersOf (symbol) }

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

    override suspend fun unsubscribeTrades(symbol: String) {
        TODO("Not yet implemented")
    }

    override suspend fun subscribeOrderBook(symbol: String): Flow<OrderBook> {

        val service : DYDXOrderBookStream by inject { parametersOf (symbol) }

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


    override suspend fun unsubscribeOrderBook(symbol: String) {
        TODO("Not yet implemented")
    }

    /**
     * Order Service
     */
    override fun createOrder(symbol: String, type: Type, side: Side, price: Float, size: Float) {
        TODO("Not yet implemented")
    }

    /**
     * Account Service
     */
    override suspend fun subscribeAccountUpdates(): Flow<AccountSnapshot> {

        //val service : DYDXAccountStream by inject()

//        // connect to service
//        log.info("Creating websocket for account updates")
//        when(service.connect()) {
//            is Resource.Success -> {
//
//                // subscribe to symbol
//                log.info("Subscribing to account updates")
//                return when (service.subscribe()) {
//                    is Resource.Success -> {
//
//                        // observe updates
//                        log.info("Listening for updates")
//                        service.observeUpdates()
//                    }
//                    is Resource.Error -> {
//                        log.error("Failed to subscribe")
//                        flow {  }
//                    }
//                }
//            }
//            is Resource.Error -> {
//                log.error("Failed to create websocket")
//                return flow {  }
//            }
//        }

        return flow {}
    }

    override suspend fun unsubscribeAccountUpdate(): Flow<AccountSnapshot> {
        TODO("Not yet implemented")
    }
}