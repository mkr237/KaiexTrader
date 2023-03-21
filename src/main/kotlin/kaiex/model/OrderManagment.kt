package com.kaiex.model

enum class Type {
    MARKET,
    LIMIT
}

data class NewOrder(val symbol: String)
data class CancelOrder(val orderId: String)
data class OrderUpdate(val orderId: String, val symbol: String)