package kaiex.exchange.simulator

import kaiex.model.*
import java.math.BigDecimal
import java.util.UUID
import kotlin.math.absoluteValue

class PositionTracker(val symbol:String) {

    var positions: MutableMap<String, Position> = mutableMapOf()
    val positionList : List<Position>
        get() = positions.values.toList()

    fun addTrade(trade: OrderFill) {

        val openPosition = openPosition()
        if (openPosition != null) {

            // if we're increasing a position
            if (openPosition.isLong() && trade.isBuy() || openPosition.isShort() && trade.isSell()) {
                positions[openPosition.positionId] = Position(
                    positionId = openPosition.positionId,
                    accountId = openPosition.accountId,
                    symbol = openPosition.symbol,
                    side = openPosition.side,
                    status = openPosition.status,
                    size = openPosition.size + trade.size,
                    maxSize = openPosition.size + trade.size,
                    entryPrice = (openPosition.entryPrice * openPosition.sumOpen + trade.price * trade.size) / (openPosition.sumOpen + trade.size),
                    exitPrice = openPosition.exitPrice,
                    openTransactionId = openPosition.openTransactionId,
                    closeTransactionId = openPosition.closeTransactionId,
                    lastTransactionId = trade.fillId,
                    closedAt = openPosition.closedAt,
                    updatedAt = trade.createdAt,
                    createdAt = openPosition.createdAt,
                    sumOpen = openPosition.sumOpen + trade.size,
                    sumClose = openPosition.sumClose,
                    netFunding = 0f,
                    realizedPnl = openPosition.realizedPnl,
                    unrealisedPnl = openPosition.unrealisedPnl
                )

            // if we're decreasing or closing a position
            } else if (isLong() && trade.isSell() || isShort() && trade.isBuy()) {

                // if the fill size is equal or bigger than the current position, close the existing position
                if (trade.size >= openPosition.size) {

                    // close existing position
                    val exitPrice = (openPosition.exitPrice * openPosition.sumClose + trade.price * trade.size) / (openPosition.sumClose + trade.size)
                    val sumClose = openPosition.sumClose + openPosition.size

                    positions[openPosition.positionId] = Position(
                        positionId = openPosition.positionId,
                        accountId = openPosition.accountId,
                        symbol = openPosition.symbol,
                        side = openPosition.side,
                        status = PositionStatus.CLOSED,
                        size = 0f,
                        maxSize = openPosition.maxSize,
                        entryPrice = openPosition.entryPrice,
                        exitPrice = exitPrice,
                        openTransactionId = openPosition.openTransactionId,
                        closeTransactionId = trade.fillId,
                        lastTransactionId = trade.fillId,
                        closedAt = trade.createdAt,
                        updatedAt = trade.createdAt,
                        createdAt = openPosition.createdAt,
                        sumOpen = openPosition.sumOpen,
                        sumClose = sumClose,
                        netFunding = 0f,
                        realizedPnl = (exitPrice - openPosition.entryPrice) * sumClose,
                        unrealisedPnl = openPosition.unrealisedPnl
                    )

                    // if trade size is bigger, open a new opposite position with the difference
                    if (trade.size > openPosition.size) {

                        // open a new (opposite) position
                        val positionId = UUID.randomUUID().toString()
                        positions[positionId] = Position(
                            positionId = positionId,
                            accountId = "0",
                            symbol = symbol,
                            side = orderSideToPositionSide(trade.side),
                            status = PositionStatus.OPEN,
                            size = trade.size - openPosition.size,
                            maxSize = trade.size - openPosition.size,
                            entryPrice = trade.price,
                            exitPrice = 0f,
                            openTransactionId = trade.fillId,
                            closeTransactionId = "",
                            lastTransactionId = trade.fillId,
                            closedAt = 0,
                            updatedAt = trade.createdAt,
                            createdAt = trade.createdAt,
                            sumOpen = trade.size - openPosition.size,
                            sumClose = 0f,
                            netFunding = 0f,
                            realizedPnl = 0f,
                            unrealisedPnl = 0f
                        )
                    }

                } else {
                    // just reduce the existing position
                    positions[openPosition.positionId] = Position(
                        positionId = openPosition.positionId,
                        accountId = openPosition.accountId,
                        symbol = openPosition.symbol,
                        side = openPosition.side,
                        status = openPosition.status,
                        size = openPosition.size - trade.size,
                        maxSize = openPosition.maxSize,
                        entryPrice = openPosition.entryPrice,
                        exitPrice = (openPosition.exitPrice * openPosition.sumClose + trade.price * trade.size) / (openPosition.sumClose + trade.size),
                        openTransactionId = openPosition.openTransactionId,
                        closeTransactionId = openPosition.closeTransactionId,
                        lastTransactionId = trade.fillId,
                        closedAt = openPosition.closedAt,
                        updatedAt = trade.createdAt,
                        createdAt = openPosition.createdAt,
                        sumOpen = openPosition.sumOpen,
                        sumClose = openPosition.sumClose + trade.size,
                        netFunding = 0f,
                        realizedPnl = openPosition.realizedPnl,     // TODO - calculate P&L on what's been closed
                        unrealisedPnl = openPosition.unrealisedPnl
                    )
                }
            }

        } else {

            // create a new position
            val positionId = UUID.randomUUID().toString()
            positions[positionId] = Position(
                positionId = positionId,
                accountId = "0",
                symbol = symbol,
                side = orderSideToPositionSide(trade.side),
                status = PositionStatus.OPEN,
                size = trade.size,
                maxSize = trade.size,
                entryPrice = trade.price,
                exitPrice = 0f,
                openTransactionId = trade.fillId,
                closeTransactionId = "",
                lastTransactionId = trade.fillId,
                closedAt = 0,
                updatedAt = trade.createdAt,
                createdAt = trade.createdAt,
                sumOpen = trade.size,
                sumClose = 0f,
                netFunding = 0f,
                realizedPnl = 0f,
                unrealisedPnl = 0f
            )
        }
    }

    private fun orderSideToPositionSide(side: OrderSide) = if(side == OrderSide.BUY) PositionSide.LONG else PositionSide.SHORT
    private fun openPosition() = positions.values.firstOrNull { it.status == PositionStatus.OPEN }
    private fun isLong() = openPosition()?.isLong() ?: false
    private fun isShort() = openPosition()?.isShort() ?: false
    private fun isFlat() = positions.values.none { it.status == PositionStatus.OPEN }
}