package kaiex.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kaiex.core.ReportManager
import kaiex.core.TradeManager
import kaiex.ui.Fill
import kaiex.ui.Order
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
                    Fill(fill.fillId, fill.price, fill.size, fill.fee, fill.role.toString(), fill.createdAt, fill.updatedAt)
                } ?: emptyList()

                Order(
                    order.orderId, order.exchangeId, order.accountId, order.symbol, order.type.toString(), order.side.toString(),
                    order.price, order.size, order.remainingSize, order.status.toString(), order.timeInForce.toString(),
                    order.createdAt, order.expiresAt,
                    orderFills.toMutableList()
                )
            }.associateBy { it.orderId }

            call.respond(orders)
        }

        route.get("/positions") {
            call.respond(tradeManager.positionMap)
        }
    }
}