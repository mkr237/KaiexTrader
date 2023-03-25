package com.kaiex

import com.kaiex.services.dydx.DYDXOrderBookStream
import com.kaiex.services.dydx.DYDXExchangeService
import com.kaiex.services.dydx.DYDXTradeStream
import kaiex.core.MarketDataManager
import kaiex.core.OrderManager
import kaiex.core.RiskManager
import kaiex.exchange.dydx.DYDXAccountStream
import org.koin.dsl.module

val core = module {
    single { MarketDataManager() }
    single { OrderManager() }
    single { RiskManager() }
}

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { params -> DYDXAccountStream() }
    factory { params -> DYDXTradeStream(symbol = params.get()) }
    factory { params -> DYDXOrderBookStream(symbol = params.get()) }

    // props can used with getProperty("my_property")
}