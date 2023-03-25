package kaiex.core

import kaiex.exchange.dydx.DYDXExchangeService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val dydxExchangeService : DYDXExchangeService by inject()

//    fun subscribeAccountUpdates() {
//        log.info("Subscribing to account updates")
//        dydxExchangeService.subscribeAccountUpdates().onEach { update ->
//            log.info(update.toString())
//        }.launchIn(this)
//    }

    fun unsubscribeAccountsUpdates() {

    }
}