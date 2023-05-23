package kaiex.exchange

import kaiex.model.AccountService
import kaiex.model.MarketDataService
import kaiex.model.OrderService

interface ExchangeService: MarketDataService, OrderService, AccountService

class ExchangeException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    // Optionally add custom properties or methods as needed
}