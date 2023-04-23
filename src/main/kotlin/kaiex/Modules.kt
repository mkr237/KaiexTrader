package kaiex

import kaiex.core.*
import kaiex.exchange.dydx.*
import kaiex.ui.UIServer
import org.koin.dsl.module

val core = module {
    single { Kaiex() }
    single { AccountManager() }
    single(createdAtStart = true) { MarketDataManager() }
    single(createdAtStart = true) { OrderManager() }
    single { RiskManager() }
    single { ReportManager() }
    single { UIServer() }
}

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { DYDXMarketsSocketEndpoint() }
    factory { DYDXAccountSocketEndpoint() }
    factory { params -> DYDXTradeSocketEndpoint(symbol = params.get()) }
    factory { params -> DYDXOrderBookSocketEndpoint(symbol = params.get()) }
    factory { DYDXOrderEndpoint() }
    // props can be used with getProperty("my_property")
}