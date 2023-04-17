package kaiex.exchange.dydx

import com.fersoft.signature.Signature
import com.fersoft.signature.StarkSigner
import com.fersoft.types.*
import kaiex.model.*
import kaiex.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*


class DYDXExchangeService: KoinComponent, MarketDataService, OrderService, AccountService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

    /**
     * Market Data Service
     */

    override suspend fun subscribeTrades(symbol: String): Flow<Trade> {

        val service : DYDXTradeSocket by inject { parametersOf (symbol) }

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

        val service : DYDXOrderBookSocket by inject { parametersOf (symbol) }

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
    override suspend fun createOrder(symbol: String, type: OrderType, side: OrderSide, price: Float, size: Float) {
        val service : DYDXOrderEndpoint by inject()

        log.info("Creating order for $symbol...") // TODO

        val starkPublicKey = "04832957876ceb5bd21d203de44e1700536baab7b1484f704db25f93fe6c67c0"
        val starkPrivateKey = "01e31a364044021297c0e7e1f5a1a20a42558537a1ba7decaf139e43a7e84b6e"  // TODO DO NOT PUSH!!!
        val PRIVATE_KEY = BigInteger(starkPrivateKey, 16)

        val clientId = UUID.randomUUID().toString()

        val order = Order(
            "1812",
            size.toString(),
            "0.015",
            DydxMarket.BTC_USD,
            StarkwareOrderSide.BUY,
            "2023-09-20T00:00:00.000Z"
        )

        val o1 = OrderWithClientIdAndQuoteAmount(order, clientId, "20000")
        val o2 = OrderWithClientId(order, clientId)

        val starkSigner = StarkSigner()
        val signature: Signature = starkSigner.sign(o1, NetworkId.GOERLI, PRIVATE_KEY)

        val data = mapOf(
            "market" to symbol,
            "side" to OrderSide.BUY.toString(),
            "type" to OrderType.LIMIT.toString(),
            "size" to size.toString(),
            "price" to "20000",
            "timeInForce" to "GTT",
            "postOnly" to "false",
            "clientId" to clientId,
            "limitFee" to "0.015",
            "expiration" to "2023-09-20T00:00:00.000Z",
            "signature" to signature.toString()
        )

        service.post("/v3/orders", data)
    }

    /**
     * Account Service
     */
    override suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate> {

        val service : DYDXAccountSocket by inject()

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

    override suspend fun unsubscribeAccountUpdate(): Flow<AccountUpdate> {
        TODO("Not yet implemented")
    }
}