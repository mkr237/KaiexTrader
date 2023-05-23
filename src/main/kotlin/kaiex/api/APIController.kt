package kaiex.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kaiex.model.ReportManager
import kaiex.model.TradeManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class APIController: KoinComponent {

    private val tradeManager: TradeManager by inject()
    private val reportManager: ReportManager by inject()

    fun registerRoutes(route: Route) {
        route.get("/metrics") {
            call.respond(tradeManager.getTradeMetrics())
        }

        route.get("/chart") {
            val chart = reportManager.charts["Default"]
            if(chart != null)
                call.respond(chart.toJson())
        }

        route.get("/orders") {
            val orders = tradeManager.orderMap.values.map { order ->
                val orderFills = tradeManager.fillMap[order.orderId]?.map { fill ->
                    Fill(fill.fillId, fill.price.toDouble(), fill.size.toDouble(), fill.fee.toDouble(), fill.role.toString(),
                        fill.createdAt.toEpochMilli(), fill.updatedAt.toEpochMilli())
                } ?: emptyList()

                Order(
                    order.orderId, order.exchangeId, order.accountId, order.symbol, order.type.toString(), order.side.toString(),
                    order.price.toDouble(), order.size.toDouble(), order.remainingSize.toDouble(), order.status.toString(),
                    order.timeInForce.toString(), order.createdAt.toEpochMilli(), order.expiresAt.toEpochMilli(),
                    orderFills.toMutableList()
                )
            }.associateBy { it.orderId }

            call.respond(orders)
        }

        route.get("/positions") {
            val positionMap = tradeManager.positionMap.mapValues { (_, innerMap) ->
                innerMap.mapValues { (_, position) ->
                    Position(
                        position.positionId,
                        position.accountId,
                        position.symbol,
                        position.side.toString(),
                        position.status.toString(),
                        position.size.toDouble(),
                        position.size.toDouble(),
                        position.entryPrice.toDouble(),
                        position.exitPrice.toDouble(),
                        position.openTransactionId,
                        position.closeTransactionId,
                        position.lastTransactionId,
                        position.closedAt?.toEpochMilli() ?: 0L,
                        position.updatedAt.toEpochMilli(),
                        position.createdAt.toEpochMilli(),
                        position.sumOpen.toDouble(),
                        position.sumClose.toDouble(),
                        position.netFunding.toDouble(),
                        position.unrealisedPnl.toDouble(),
                        position.realizedPnl.toDouble()
                    )
                }.toMutableMap()
            }

            call.respond(positionMap)
        }
    }
}