package kaiex.exchange

import kaiex.model.AccountService
import kaiex.model.MarketDataService
import kaiex.model.OrderService

interface ExchangeService: MarketDataService, OrderService, AccountService