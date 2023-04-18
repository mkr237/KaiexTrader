package kaiex.exchange.dydx

import kaiex.util.Resource
import kotlinx.coroutines.flow.Flow

interface DYDXSocketEndpoint<T> {

    companion object {
        //const val BASE_URL = "wss://api.dydx.exchange"
        const val BASE_URL = "wss://api.stage.dydx.exchange"
    }

    sealed class Endpoints(val base: String, val ext: String) {
        object DYDXSocket: Endpoints("$BASE_URL", "/v3/ws")

        fun getURL() = base + ext
    }

    suspend fun connect(): Resource<Unit>
    suspend fun subscribe(): Resource<Unit>
    fun observeUpdates(): Flow<T>
    suspend fun disconnect()
}