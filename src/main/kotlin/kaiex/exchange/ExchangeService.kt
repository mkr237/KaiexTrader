package kaiex.exchange

import kaiex.model.AccountInfo
import kaiex.model.OrderBook
import kaiex.model.Trade
import kotlinx.coroutines.flow.Flow

interface ExchangeService {
    suspend fun subscribeToTrades(symbol: String): Flow<Trade>
    suspend fun subscribeToOrderBook(symbol: String): Flow<OrderBook>
    suspend fun subscribeToAccountUpdates(): Flow<AccountInfo>

//    suspend fun placeOrder(symbol: String, type:Type, side:Side, price: Float, size: Float): Flow<OrderUpdate>
}