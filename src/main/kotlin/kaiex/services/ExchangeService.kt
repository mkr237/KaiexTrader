package com.kaiex.services

import com.kaiex.model.*
import kotlinx.coroutines.flow.Flow

interface ExchangeService {
    suspend fun subscribeToTrades(symbol: String): Flow<Trade>
    suspend fun subscribeToOrderBook(symbol: String): Flow<OrderBook>
    suspend fun placeOrder(symbol: String, type:Type, side:Side, price: Float, size: Float): Flow<OrderUpdate>
}