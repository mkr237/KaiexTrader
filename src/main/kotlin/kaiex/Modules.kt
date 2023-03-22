package com.kaiex

import com.kaiex.services.dydx.DYDXOrderBookStream
import com.kaiex.services.dydx.DYDXExchangeService
import com.kaiex.services.dydx.DYDXTradeStream
import kaiex.services.dydx.DYDXAccountStream
import org.koin.dsl.module

val dydxExchangeService = module {
    single { DYDXExchangeService() }
    factory { params -> DYDXAccountStream() }
    factory { params -> DYDXTradeStream(symbol = params.get()) }
    factory { params -> DYDXOrderBookStream(symbol = params.get()) }

    // props can used with getProperty("my_property")
}