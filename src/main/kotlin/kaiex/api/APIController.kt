package kaiex.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kaiex.core.OrderManager
import kaiex.core.ReportManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class APIController: KoinComponent {

    private val orderManager: OrderManager by inject()
    private val reportManager: ReportManager by inject()
    fun registerRoutes(route: Route) {
        route.get("/status") {
            val strategyList = listOf("one", "two", "three")
            call.respond(strategyList)
        }

        route.get("/chart") {
            val chart = reportManager.charts["Default"]
            if(chart != null)
                call.respond(chart.toJson())
        }

        route.get("/orders") {
            call.respond(orderManager.orderMap)
        }

        route.get("/fills") {
            call.respond(orderManager.fillMap)
        }

        route.get("/positions") {
            call.respond(orderManager.positionMap)
        }

        route.get("/marketdata") {
            // Implement your logic to retrieve market data here
        }

        // Add more routes as needed
    }
}