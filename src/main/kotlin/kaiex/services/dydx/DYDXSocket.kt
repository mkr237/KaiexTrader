package com.kaiex.services.dydx

import com.kaiex.model.Trade
import com.kaiex.util.Resource
import kotlinx.coroutines.flow.Flow

interface DYDXSocket<T> {
    companion object {
        const val BASE_URL = "wss://api.dydx.exchange/v3"
    }

    sealed class Endpoints(val url: String) {
        object DYDXSocket: Endpoints("$BASE_URL/ws")
    }

    suspend fun connect(): Resource<Unit>
    suspend fun subscribe(): Resource<Unit>
    fun observeUpdates(): Flow<T>
    suspend fun disconnect()
}