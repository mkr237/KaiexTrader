package kaiex.exchange.dydx

import kaiex.util.Resource
import kotlinx.coroutines.flow.Flow

interface DYDXSocket<T> {
    companion object {
        const val BASE_URL = "wss://api.stage.dydx.exchange/v3"
    }

    sealed class Endpoints(val url: String) {
        object DYDXSocket: Endpoints("$BASE_URL/ws")
    }

    suspend fun connect(): Resource<Unit>
    suspend fun subscribe(): Resource<Unit>
    fun observeUpdates(): Flow<T>
    suspend fun disconnect()
}