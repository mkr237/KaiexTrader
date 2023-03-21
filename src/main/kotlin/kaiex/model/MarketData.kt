package com.kaiex.model

import java.time.Instant

enum class Side {
    BUY,
    SELL
}

data class Candle (
    val startTimestamp: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String
)

data class Trade (
    val symbol:String,
    val side:Side,
    val size:Float,
    val price:Float,
    val createdAt:Instant,
    val liquidation:Boolean
)

data class OrderBookEntry (
    val size:Float,
    val price:Float
)

data class OrderBook (
    val symbol:String,
    val bids:List<OrderBookEntry>,
    val asks:List<OrderBookEntry>,
    val receivedTime:Instant
)
