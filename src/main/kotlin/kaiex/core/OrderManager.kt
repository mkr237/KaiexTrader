package kaiex.core

import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.OrderSide
import kaiex.model.OrderType
import kaiex.model.OrderUpdate
import kaiex.model.Trade
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OrderManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val dydxExchangeService : DYDXExchangeService by inject()

    fun createOrder(symbol: String, type: OrderType, side: OrderSide, price: Float, size: Float): Flow<OrderUpdate> {

        log.info("Creating order for $symbol...")
        CoroutineScope(Dispatchers.Default).launch {
            dydxExchangeService.createOrder(symbol, type, side, price, size)
        }

        return flowOf()
    }
}