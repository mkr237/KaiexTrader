package kaiex

import kaiex.strategy.KaiexStrategy
import kaiex.strategy.StrategyException
import kaiex.ui.StrategyConfig
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

/**
 * Loads and starts the strategy specified in the provided config
 */
class StrategyRunner(private val config: StrategyConfig) {
    private val log:Logger = LoggerFactory.getLogger(javaClass.simpleName)
    private var strategy: KaiexStrategy? = null

    fun start() = runBlocking {
        log.info("Starting strategy ${config.strategyType}")
        strategy = loadStrategy(config.strategyType)
        launch { strategy?.onCreate(config) }
    }

    fun stop() = runBlocking {
        log.info("Stopping strategy ${config.strategyType}")
        launch { strategy?.onDestroy() }
    }

    private fun loadStrategy(className: String): KaiexStrategy? {
        log.info("Loading strategy $className")
        return try {
            val kClass = Class.forName(className).kotlin
            kClass.createInstance() as? KaiexStrategy
        } catch (e: Exception) {
            log.error("Error loading strategy: $className")
            e.printStackTrace()
            null
        }
    }
}