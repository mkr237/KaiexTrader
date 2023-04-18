package kaiex.core

import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.*
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class OrderManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val dydxExchangeService : DYDXExchangeService by inject()

    suspend fun createOrder(symbol: String,
                    type: OrderType,
                    side: OrderSide,
                    price: Float,
                    size: Float,
                    limitFee: Float,
                    timeInForce: OrderTimeInForce): Result<String> {

        val order = CreateOrder(
            UUID.randomUUID().toString(),
            "DYDX",
            symbol,
            type,
            side,
            price,
            size,
            limitFee,
            timeInForce,
            false,
            false,
            Instant.now().epochSecond
        )

        log.info("Creating new order: $order")
        return dydxExchangeService.createOrder(order)
    }
}