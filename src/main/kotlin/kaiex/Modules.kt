package kaiex

import kaiex.api.APIController
import kaiex.core.*
import kaiex.exchange.ExchangeService
import kaiex.exchange.dydx.*
import kaiex.exchange.simulator.SimulatorService
import kaiex.core.MarketDataManager
import org.koin.dsl.module

val core = module {
    single(createdAtStart = true) { MarketDataManager() }
    single(createdAtStart = true) { OrderManager() }
    single(createdAtStart = true) { ReportManager() }
}

val dydxExchangeService = module {

    // check for DYDX environment variables
    val requiredEnvVars = listOf("DYDX_API_KEY", "DYDX_API_PASSPHRASE", "DYDX_API_SECRET", "ETHEREUM_ADDRESS", "STARK_PRIVATE_KEY")
    val missingEnvVars = requiredEnvVars.filter { System.getenv(it).isNullOrBlank() }
    if (missingEnvVars.isNotEmpty()) {
        val missingVarsMessage = "Missing environment variables: ${missingEnvVars.joinToString()}"
        throw IllegalStateException(missingVarsMessage)
    }

    single<ExchangeService> { DYDXExchangeService() }
    factory { DYDXMarketsSocketEndpoint() }
    factory { DYDXAccountSocketEndpoint() }
    factory { params -> DYDXTradeSocketEndpoint(symbol = params.get()) }
    factory { params -> DYDXOrderBookSocketEndpoint(symbol = params.get()) }
    factory { DYDXOrderEndpoint() }
    // props can be used with getProperty("my_property")
}

val binanceExchangeSimulator = module {
    single<ExchangeService> { SimulatorService() }
}

val apiModule = module {
    single { APIController() }
}