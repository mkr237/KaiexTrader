package kaiex.exchange.dydx

import kaiex.model.CreateOrder

interface DYDXHttpEndpoint<T> {

    companion object {
        //const val BASE_URL = "https://api.dydx.exchange"
        const val BASE_URL = "https://api.stage.dydx.exchange"
    }

    sealed class Endpoints(val base: String, val ext: String) {
        object DYDXAccounts: Endpoints("$BASE_URL", "/v3/accounts")
        object DYDXOrders: Endpoints("$BASE_URL", "/v3/orders")

        fun getURL() = base + ext
    }

    suspend fun get(): Result<T>
    suspend fun post(order: CreateOrder): Result<T>
}