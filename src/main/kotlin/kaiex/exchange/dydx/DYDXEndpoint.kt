package kaiex.exchange.dydx

import kaiex.util.Resource

interface DYDXEndpoint<T> {
    companion object {
        //const val BASE_URL = "https://api.dydx.exchange/v3"
        const val BASE_URL = "https://api.stage.dydx.exchange/v3"
    }

    sealed class Endpoints(val url: String) {
        object DYDXEndpoint: Endpoints("$BASE_URL")
    }

    suspend fun get(url: String): Resource<T>
    suspend fun post(url: String, data: Map<String, String>): Resource<T>
}