package kaiex.exchange.simulator

import kaiex.model.OrderFill
import kaiex.model.OrderSide
import java.math.BigDecimal

class PositionTracker(var positionSize: BigDecimal = BigDecimal.ZERO,
                      var avgEntryPrice: BigDecimal = BigDecimal.ZERO,
                      var avgExitPrice: BigDecimal = BigDecimal.ZERO,
                      var totalExitSize: BigDecimal = BigDecimal.ZERO,
                      var realizedPnl: BigDecimal = BigDecimal.ZERO,
                      var unrealizedPnl: BigDecimal = BigDecimal.ZERO,
                      var marketPrice: BigDecimal? = null) {

    fun addTrade(trade: OrderFill) {
        val fillPrice = trade.price.toBigDecimal()
        val fillSize = if (trade.side == OrderSide.BUY) trade.size.toBigDecimal() else -trade.size.toBigDecimal()
        val fillCost = fillSize * trade.price.toBigDecimal()

        if (isFlat()) {
            // starting a new position
            positionSize = fillSize
            avgEntryPrice = fillPrice
            avgExitPrice = BigDecimal.ZERO
            totalExitSize = BigDecimal.ZERO

        } else if (isLong() && fillSize > BigDecimal.ZERO || isShort() && fillSize < BigDecimal.ZERO) {
            // adding to a position
            increasePosition(fillSize, fillCost)

        } else if(isLong() && fillSize < BigDecimal.ZERO || isShort() && fillSize > BigDecimal.ZERO) {

            // if the fill size is bigger than the current position, close the existing position then open a new one with the remainder
            if(isLong() && (positionSize + fillSize) < BigDecimal.ZERO || isShort() && (positionSize + fillSize) > BigDecimal.ZERO) {

                val fillSizeClose = -positionSize
                val fillCostClose = fillSizeClose * trade.price.toBigDecimal()
                val fillSizeOpen = positionSize + fillSize
                val fillCostOpen = fillSizeOpen * trade.price.toBigDecimal()
                reducePosition(fillSizeClose, fillCostClose)
                increasePosition(fillSizeOpen, fillCostOpen)

            } else {
                reducePosition(fillSize, fillCost)
            }
        }

        // update the P&L
        updatePnl()
    }

    fun updatePrice(price: Float) {
        marketPrice = price.toBigDecimal()
        updatePnl()
    }

    private fun increasePosition(fillSize:BigDecimal, fillCost:BigDecimal) {
        val newAvgEntryPrice = (avgEntryPrice * positionSize + fillCost) / (positionSize + fillSize)
        positionSize += fillSize
        avgEntryPrice = newAvgEntryPrice
    }

    private fun reducePosition(fillSize:BigDecimal, fillCost:BigDecimal) {
        assert(fillSize.abs() <= positionSize.abs())
        avgExitPrice = (avgExitPrice * totalExitSize + fillCost.abs()) / (totalExitSize + fillSize.abs())
        positionSize += fillSize
        totalExitSize += fillSize.abs()
    }

    private fun updatePnl() {
        if(marketPrice != null) {
            unrealizedPnl = positionSize * (marketPrice!! - avgEntryPrice)
            // TODO realised
        } else {
            unrealizedPnl = BigDecimal.ZERO
            realizedPnl = BigDecimal.ZERO
        }
    }

    private fun isLong() = positionSize > BigDecimal.ZERO
    private fun isShort() = positionSize < BigDecimal.ZERO
    private fun isFlat() = positionSize.compareTo(BigDecimal.ZERO) == 0
}