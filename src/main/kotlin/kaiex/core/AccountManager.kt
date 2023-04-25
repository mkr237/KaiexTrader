package kaiex.core

import kaiex.exchange.ExchangeService
import kaiex.exchange.dydx.DYDXExchangeService
import kaiex.model.AccountUpdate
import kaiex.util.EventBroadcaster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AccountManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private val exchangeService : ExchangeService by inject()

    private val accountBroadcasters:MutableMap<String, EventBroadcaster<AccountUpdate>> = mutableMapOf()

    fun subscribeAccountUpdates(accountId: String): EventBroadcaster<AccountUpdate> {
        if(!accountBroadcasters.containsKey(accountId)) {
            log.info("Subscribing to account updates for account:$accountId")
            accountBroadcasters[accountId] = EventBroadcaster()
            CoroutineScope(Dispatchers.Default).launch {
                exchangeService.subscribeAccountUpdates((accountId)).collect { update:AccountUpdate ->
                    accountBroadcasters[accountId]?.sendEvent(update)
                }
            }
        } else {
            log.info("Account subscription exists for $accountId")
        }

        return accountBroadcasters[accountId] ?: throw RuntimeException("Unknown Account: $accountId")
    }

    fun unsubscribeAccountsUpdates() {

    }
}