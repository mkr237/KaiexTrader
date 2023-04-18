package kaiex.exchange.dydx

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
import kotlin.reflect.typeOf


class DYDXExchangeService: KoinComponent, MarketDataService, OrderService, AccountService {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)

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

    override suspend fun unsubscribeTrades(symbol: String) {
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


    override suspend fun unsubscribeOrderBook(symbol: String) {
        TODO("Not yet implemented")
    }

    /**
     * Order Service
     */
    override suspend fun createOrder(order: CreateOrder):Result<String> {

        val service : DYDXOrderEndpoint by inject()

        log.info("Creating order: $order")
        return service.post(order)


//        val positionId = "5630"  // TODO get from account
//        val symbol = DydxMarket.fromString(symbol)
//        val size = size
//        val price = price
//        val side = side
//        val type = type
//        val limitFee = limitFee
//        val timeInForce = timeInForce
//        val expiration = getISOTime(plusMins = 60)
//        val clientId = UUID.randomUUID().toString()

//        // create Stark signature for the order
//        val starkPrivateKey = System.getenv("STARK_PRIVATE_KEY")
//        val startkPrivateKeyInt = BigInteger(starkPrivateKey, 16)
//        val order = Order(positionId, size.toString(), limitFee.toString(), symbol, StarkwareOrderSide.valueOf(side.toString()), expiration)
//        val orderWithPrice = OrderWithClientIdWithPrice(order, clientId, price.toString())
//        val signature = StarkSigner().sign(orderWithPrice, NetworkId.GOERLI, startkPrivateKeyInt)

//        val data = mapOf(
//            "market" to symbol.toString(),
//            "side" to side.toString(),
//            "type" to type.toString(),
//            "timeInForce" to timeInForce.toString(),
//            "size" to size.toString(),
//            "price" to price.toString(),
//            "limitFee" to limitFee.toString(),
//            "expiration" to expiration,
//            "postOnly" to "false",
//            "clientId" to clientId,
//            "signature" to signature.toString(),
//            "reduceOnly" to "false"
//        )

//        service.post(data)
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

    override suspend fun unsubscribeAccountUpdate(): Flow<AccountUpdate> {
        TODO("Not yet implemented")
    }
}