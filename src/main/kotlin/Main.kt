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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {

    // check args
    if (args.size < 2) {
        log.error("Usage: engine [command] [strategy] [parameters]")
        return
    }

    // pass args
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

    // run strategy
    CoroutineScope(Dispatchers.Default).launch {

        // load required modules
        startKoin {
            modules(core)
            modules(apiModule)
            when (command) {
                "run" -> { modules(dydxExchangeService) }
                "backtest" -> { modules(binanceExchangeSimulator) }
                else -> log.error("Invalid command: $command")
            }
        }

        // run the strategy
        val runner = StrategyRunner(strategy, parameters)
        runner.start()
        runner.stop()
    }

    // start web server
    embeddedServer(Netty, port = 8080) {
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