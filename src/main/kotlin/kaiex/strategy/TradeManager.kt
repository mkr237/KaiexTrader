package kaiex.strategy

import kaiex.model.OrderFill
import kaiex.model.OrderSide
import kotlin.math.abs

class TradeManager(val symbol: String,
                   var positionSize: Float = 0f,
                   var avgEntryPrice: Float = 0f,
                   var avgExitPrice: Float = 0f,
                   var realizedPnl: Float = 0f,
                   var unrealizedPnl: Float = 0f) {

    fun onTrade(trade: OrderFill) {
        require(trade.symbol == symbol) { "Trade symbol must match TradeManager symbol" }
        val fillSize = if (trade.side == OrderSide.BUY) trade.size else -trade.size
        val fillCost = fillSize * trade.price

        if (positionSize == 0f) {
            positionSize = fillSize
            avgEntryPrice = trade.price

        // extending long
        } else if (positionSize > 0f && fillSize > 0f) {
            val newAvgEntryPrice = (avgEntryPrice * positionSize + fillCost) / (positionSize + fillSize)
            positionSize += fillSize
            avgEntryPrice = newAvgEntryPrice

        // extending short
        } else if (positionSize < 0f && fillSize < 0f) {
            val newAvgEntryPrice = (avgEntryPrice * positionSize - fillCost) / (positionSize - fillSize)
            positionSize += fillSize
            avgEntryPrice = newAvgEntryPrice

        // reducing position
        } else {
            val realizedPnlAmount = fillSize * (avgEntryPrice - trade.price)
            realizedPnl += realizedPnlAmount
            positionSize += fillSize
            if (positionSize == 0f) {
                avgExitPrice = 0f
                unrealizedPnl = 0f
            } else {
                if (positionSize > 0f) {
                    var totalExitCost = (avgExitPrice) * (positionSize - fillSize)
                    totalExitCost += trade.price * fillSize
                    avgExitPrice = totalExitCost / positionSize
                }
                unrealizedPnl = positionSize * (trade.price - avgEntryPrice)
            }
        }
    }

//    fun onPriceUpdate(price: Float) {
//        if (positionSize != 0f) {
//            unrealizedPnl = positionSize * (price - avgEntryPrice)
//        }
//    }
//
//    fun getPositionSize(): Float {
//        return positionSize
//    }
//
//    fun getAvgEntryPrice(): Float {
//        return avgEntryPrice
//    }
//
//    fun getAvgExitPrice(): Float? {
//        return abs(avgExitPrice)
//    }
//
//    fun getRealizedPnl(): Float {
//        return realizedPnl
//    }
//
//    fun getUnrealizedPnl(): Float {
//        return unrealizedPnl
//    }
}
