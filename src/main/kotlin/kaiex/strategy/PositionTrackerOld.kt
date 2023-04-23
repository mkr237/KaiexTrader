package kaiex.strategy

import kaiex.model.OrderFill
import kaiex.model.OrderSide
import java.math.BigDecimal

class PositionTrackerOld(val symbol: String,
                         var positionSize: BigDecimal = BigDecimal.ZERO,
                         var avgEntryPrice: BigDecimal = BigDecimal.ZERO,
                         var avgExitPrice: BigDecimal = BigDecimal.ZERO,
                         var realizedPnl: BigDecimal = BigDecimal.ZERO,
                         var unrealizedPnl: BigDecimal = BigDecimal.ZERO) {

    fun addTrade(trade: OrderFill) {
        require(trade.symbol == symbol) { "Trade symbol must match TradeManager symbol" }

        val fillPrice = trade.price.toBigDecimal()
        val fillSize = if (trade.side == OrderSide.BUY) trade.size.toBigDecimal() else -trade.size.toBigDecimal()
        val fillCost = fillSize * trade.price.toBigDecimal()

        if (positionSize == BigDecimal.ZERO) {
            // Starting a new position
            positionSize = fillSize
            avgEntryPrice = fillPrice

        // if we're currently long
        } else if (positionSize > BigDecimal.ZERO) {

            // if we're adding to our position
            if(fillSize > BigDecimal.ZERO) {
                val newAvgEntryPrice = (avgEntryPrice * positionSize + fillCost) / (positionSize + fillSize)
                positionSize += fillSize
                avgEntryPrice = newAvgEntryPrice

            // if we're reducing our position
            } else if(positionSize + fillSize > BigDecimal.ZERO) {
                val realizedPnlAmount = fillSize * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += fillSize
                avgExitPrice = fillPrice
                unrealizedPnl = positionSize * (fillPrice - avgEntryPrice)

            // if we're closing our position
            } else if(BigDecimal.ZERO.compareTo(positionSize + fillSize) == 0) {
                val realizedPnlAmount = fillSize * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += fillSize
                avgExitPrice = fillPrice
                unrealizedPnl = BigDecimal.ZERO

            // if we're closing and starting a new opposite position
            } else {
                println("creating new short position")
                val closeSize = -positionSize
                val openSize = fillSize + positionSize

                // close long position
                val realizedPnlAmount = closeSize * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += closeSize
                avgExitPrice = fillPrice
                unrealizedPnl = BigDecimal.ZERO

                // start new short position
                positionSize = openSize
                avgEntryPrice = fillPrice
            }

        } else if (positionSize < BigDecimal.ZERO) {

            // if we're adding to our position
            if(fillSize < BigDecimal.ZERO) {
                val newAvgEntryPrice = (avgEntryPrice * positionSize - fillCost) / (positionSize - fillSize)
                positionSize += fillSize
                avgEntryPrice = newAvgEntryPrice

            // if we're reducing our position
            } else if(positionSize + fillSize > BigDecimal.ZERO) {
                val realizedPnlAmount = fillSize * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += fillSize
                avgExitPrice = fillPrice
                unrealizedPnl = positionSize * (fillPrice - avgEntryPrice)

            // if we're closing our position
            } else if(BigDecimal.ZERO.compareTo(positionSize + fillSize) == 0) {
                val realizedPnlAmount = fillSize * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += fillSize
                avgExitPrice = fillPrice
                unrealizedPnl = BigDecimal.ZERO

            // if we're closing and starting a new opposite position
            } else {
                println("creating new long position")

                val closePortion = -positionSize
                val openPortion = fillSize - positionSize

                // close long position
                val realizedPnlAmount = closePortion * (avgEntryPrice - fillPrice)
                realizedPnl += realizedPnlAmount
                positionSize += closePortion
                avgExitPrice = fillPrice
                unrealizedPnl = BigDecimal.ZERO

                // start new short position
                positionSize = openPortion
                avgEntryPrice = fillPrice
            }
        }
    }
//    fun addTrade(trade: OrderFill) {
//
//        require(trade.symbol == symbol) { "Trade symbol must match TradeManager symbol" }
//        val fillPrice = trade.price.toBigDecimal()
//        val fillSize = if (trade.side == OrderSide.BUY) trade.size.toBigDecimal() else -trade.size.toBigDecimal()
//        val fillCost = fillSize * trade.price.toBigDecimal()
//
//        if (positionSize == BigDecimal.ZERO) {
//            positionSize = fillSize
//            avgEntryPrice = fillPrice
//
//        // extending long
//        } else if (positionSize > BigDecimal.ZERO && fillSize > BigDecimal.ZERO) {
//            val newAvgEntryPrice = (avgEntryPrice * positionSize + fillCost) / (positionSize + fillSize)
//            positionSize += fillSize
//            avgEntryPrice = newAvgEntryPrice
//
//        // extending short
//        } else if (positionSize < BigDecimal.ZERO && fillSize < BigDecimal.ZERO) {
//            val newAvgEntryPrice = (avgEntryPrice * positionSize - fillCost) / (positionSize - fillSize)
//            positionSize += fillSize
//            avgEntryPrice = newAvgEntryPrice
//
//        // reducing position
//        } else {
//            val realizedPnlAmount = fillSize * (avgEntryPrice - fillPrice)
//            realizedPnl += realizedPnlAmount
//            positionSize += fillSize
//
//
//            if (positionSize == BigDecimal.ZERO) {
//                //avgExitPrice = BigDecimal.ZERO
//                unrealizedPnl = BigDecimal.ZERO
//            } else {
//                if (positionSize > BigDecimal.ZERO) {
//                    var totalExitCost = (avgExitPrice) * (positionSize - fillSize)
//                    totalExitCost += fillPrice * fillSize
//                    avgExitPrice = (totalExitCost / positionSize).abs()
//                }
//                unrealizedPnl = positionSize * (fillPrice - avgEntryPrice)
//            }
//        }
//    }

    fun updatePrice(price: Float) {
        if (positionSize != BigDecimal.ZERO) {
            unrealizedPnl = positionSize * (price.toBigDecimal() - avgEntryPrice)
        }
    }
}
