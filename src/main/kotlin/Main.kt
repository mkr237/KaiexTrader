import kaiex.StrategyRunner
import kaiex.binanceExchangeSimulator
import kaiex.core
import kaiex.dydxExchangeService
import kaiex.ui.ChartSeriesConfig
import kaiex.ui.StrategyConfig
import org.koin.core.context.startKoin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {

    if (args.size < 2) {
        log.error("Usage: engine [command] [configFile]")
        return
    }

    val command = args[0]
    val configFile = args[1]

    // TODO load from specified configFile
    val config = StrategyConfig(
        strategyId = "BuyAndHoldStrategy:BTC-USD",
        strategyType = "kaiex.strategy.BuyAndHoldStrategy",
        strategyDescription = "Strategy that simply buys BTC-USD and HODLs",
        symbols = listOf("BTC-USD"),
        parameters = mapOf("foo" to "bar"),
        chartConfig = listOf(
            ChartSeriesConfig("price", "candle", 0, "#00FF00"),
            ChartSeriesConfig("best-bid", "line", 0, "#2196F3"),
            ChartSeriesConfig("best-ask", "line", 0, "#FC6C02")
        )
    )

    when (command) {
        "run" -> runStrategy(config)
        "backtest" -> backtestStrategy(config)
        else -> log.error("Invalid command: $command")
    }
}

fun runStrategy(config: StrategyConfig) {
    log.info("Running strategy ${config.strategyType}")
    startKoin{
        //printLogger(Level.INFO)
        //fileProperties()
        modules(dydxExchangeService)
        modules(core)
    }

    val runner = StrategyRunner(config)
    runner.start()
    runner.stop()
}

fun backtestStrategy(config: StrategyConfig) {
    log.info("Backtesting strategy ${config.strategyType}")
    startKoin{
        //printLogger(Level.INFO)
        //fileProperties()
        modules(binanceExchangeSimulator)
        modules(core)
    }

    val runner = StrategyRunner(config)
    runner.start()
    runner.stop()
}