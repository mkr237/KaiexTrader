package kaiex

import kaiex.core.*
import kaiex.exchange.dydx.*
import kaiex.ui.UIServer
import org.koin.dsl.module

val core = module {
    //single { Kaiex() }
    single { AccountManager() }
    single(createdAtStart = true) { MarketDataManager() }
    single(createdAtStart = true) { OrderManager() }
    single { RiskManager() }
    single { ReportManager() }
    single(createdAtStart = true) { UIServer() }
}

val dydxExchangeService = module {

    // check for DYDX environment variables
    val requiredEnvVars = listOf("DYDX_API_KEY", "DYDX_API_PASSPHRASE", "DYDX_API_SECRET", "ETHEREUM_ADDRESS", "STARK_PRIVATE_KEY")
    val missingEnvVars = requiredEnvVars.filter { System.getenv(it).isNullOrBlank() }
    if (missingEnvVars.isNotEmpty()) {
        val missingVarsMessage = "Missing environment variables: ${missingEnvVars.joinToString()}"
        throw IllegalStateException(missingVarsMessage)
    }

    single { DYDXExchangeService() }
    factory { DYDXMarketsSocketEndpoint() }
    factory { DYDXAccountSocketEndpoint() }
    factory { params -> DYDXTradeSocketEndpoint(symbol = params.get()) }
    factory { params -> DYDXOrderBookSocketEndpoint(symbol = params.get()) }
    factory { DYDXOrderEndpoint() }
    // props can be used with getProperty("my_property")
}