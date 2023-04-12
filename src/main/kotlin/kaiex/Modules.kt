package kaiex

import kaiex.core.*
import kaiex.exchange.dydx.DYDXAccountStream
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.exchange.dydx.DYDXOrderBookStream
import kaiex.exchange.dydx.DYDXTradeStream
import kaiex.ui.UIServer
import org.koin.dsl.module

val core = module {
    single { Kaiex() }
    single { AccountManager() }
    single { MarketDataManager() }
    single { OrderManager() }
    single { RiskManager() }
    single { ReportManager() }
    single { UIServer() }
}

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { DYDXAccountStream() }
    factory { params -> DYDXTradeStream(symbol = params.get()) }
    factory { params -> DYDXOrderBookStream(symbol = params.get()) }

    // props can used with getProperty("my_property")
}