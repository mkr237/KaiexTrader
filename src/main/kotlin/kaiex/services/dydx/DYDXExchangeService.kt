package com.kaiex.services.dydx

import com.kaiex.model.*
import com.kaiex.services.ExchangeService
import com.kaiex.util.Resource
import kaiex.model.AccountUpdate
import kaiex.services.dydx.DYDXAccountStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DYDXExchangeService: KoinComponent, ExchangeService {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override suspend fun subscribeToTrades(symbol: String): Flow<Trade> {

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

    override suspend fun subscribeToOrderBook(symbol: String): Flow<OrderBook> {

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

    override suspend fun subscribeToAccountUpdates(): Flow<AccountUpdate> {

        val service : DYDXAccountStream by inject()

        // connect to service
        log.info("Creating websocket for account updates")
        when(service.connect()) {
            is Resource.Success -> {

                // subscribe to symbol
                log.info("Subscribing to account updates")
                return when (service.subscribe()) {
                    is Resource.Success -> {

                        // observe updates
                        log.info("Listening for updates")
                        service.observeUpdates()
                    }
                    is Resource.Error -> {
                        log.error("Failed to subscribe")
                        flow {  }
                    }
                }
            }
            is Resource.Error -> {
                log.error("Failed to create websocket")
                return flow {  }
            }
        }
    }

    override suspend fun placeOrder (
        symbol: String,
        type: Type,
        side: Side,
        price: Float,
        size: Float
    ): Flow<OrderUpdate> {
        TODO("Not yet implemented")
    }
}