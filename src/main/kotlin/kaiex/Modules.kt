package kaiex

import kaiex.core.*
import kaiex.exchange.dydx.*
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
    factory { DYDXAccountSocket() }
    factory { params -> DYDXTradeSocket(symbol = params.get()) }
    factory { params -> DYDXOrderBookSocket(symbol = params.get()) }
    factory { DYDXOrderEndpoint() }
    // props can used with getProperty("my_property")
}