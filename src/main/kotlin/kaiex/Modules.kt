package kaiex

import kaiex.core.*
import kaiex.exchange.dydx.*
import kaiex.ui.UIServer
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.core.component.get

val core = module {
    single { Kaiex() }
    single { AccountManager() }
    single { MarketDataManager() }
    single(createdAtStart = true) { OrderManager() }
    single { RiskManager() }
    single { ReportManager() }
    single { UIServer() }
}

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { DYDXAccountSocketEndpoint() }
    factory { params -> DYDXTradeSocketEndpoint(symbol = params.get()) }
    factory { params -> DYDXOrderBookSocketEndpoint(symbol = params.get()) }
    factory { DYDXOrderEndpoint() }
    // props can used with getProperty("my_property")
}