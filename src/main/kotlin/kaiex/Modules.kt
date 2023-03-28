package kaiex

import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.core.RiskManager
import kaiex.exchange.dydx.DYDXAccountStream
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.exchange.dydx.DYDXOrderBookStream
import kaiex.exchange.dydx.DYDXTradeStream
import kaiex.strategy.MACDStrategy
import org.koin.dsl.module

val core = module {
    single { Kaiex() }
    single { MarketDataManager() }
    single { OrderManager() }
    single { RiskManager() }
}

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { DYDXAccountStream() }
    factory { params -> DYDXTradeStream(symbol = params.get()) }
    factory { params -> DYDXOrderBookStream(symbol = params.get()) }

    // props can used with getProperty("my_property")
}