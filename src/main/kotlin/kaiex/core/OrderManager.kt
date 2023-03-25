package kaiex.core

import com.kaiex.services.dydx.DYDXExchangeService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OrderManager : KoinComponent {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val dydxExchangeService : DYDXExchangeService by inject()
}