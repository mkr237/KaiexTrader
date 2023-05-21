package kaiex

import kaiex.strategy.KaiexStrategy
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Loads and starts the strategy specified in the provided config
 */
class StrategyRunner(private val strategyClass: String, private val parameters: Map<String, String>) {
    private val log:Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var strategy: KaiexStrategy? = null

    suspend fun start() {
        log.info("Starting strategy $strategyClass with parameters $parameters")
        strategy = loadStrategy(strategyClass)
        strategy?.start()
    }

    suspend fun stop() {
        log.info("Stopping strategy $strategyClass")
        strategy?.stop()
    }

    private fun loadStrategy(className: String): KaiexStrategy? {
        log.info("Loading strategy class $className")
        return try {
            val constructor = Class.forName(className).kotlin.constructors.first()
            constructor.call(parameters) as KaiexStrategy

        } catch (e: Exception) {
            log.error("Error loading strategy: $className")
            e.printStackTrace()
            null
        }
    }
}