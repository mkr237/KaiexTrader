import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kaiex.*
import kaiex.api.APIController
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {

    if (args.size < 2) {
        log.error("Usage: engine [command] [strategy] [parameters]")
        return
    }

    val command = args[0]
    val strategy = args[1]
    val parameters = if (args.size > 2) {
        args[2].split(",").associate {
            val (key, value) = it.split("=")
            key to value
        }
    } else {
        emptyMap()
    }

    when (command) {
        "run" ->  runStrategy(strategy, parameters)
        "backtest" -> backtestStrategy(strategy, parameters)
        else -> log.error("Invalid command: $command")
    }

    embeddedServer(Netty, port = 8080) {
//        install(Koin) {
//
//        }
        install(WebSockets)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(CORS) {
            anyHost()
        }
        install(Routing) {
            val apiController: APIController by inject()
            route("/api") {
                apiController.registerRoutes(this)
            }
        }
    }.start(wait = true)
}

fun runStrategy(strategy: String, parameters: Map<String, String>) {
    log.info("Running strategy $strategy")
    startKoin{
        //printLogger(Level.INFO)
        //fileProperties()
        modules(dydxExchangeService)
        modules(core)
        modules(apiModule)
    }

    val runner = StrategyRunner(strategy, parameters)
    runner.start()
    runner.stop()
}

fun backtestStrategy(strategy: String, parameters: Map<String, String>) {
    log.info("Backtesting strategy $strategy")
    startKoin{
        //printLogger(Level.INFO)
        //fileProperties()
        modules(binanceExchangeSimulator)
        modules(core)
        modules(apiModule)
    }

    val runner = StrategyRunner(strategy, parameters)
    runner.start()
    runner.stop()
}
