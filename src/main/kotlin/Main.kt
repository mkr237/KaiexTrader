import com.fersoft.types.StarkwareOrderSide
import kaiex.Kaiex
import kaiex.core
import kaiex.dydxExchangeService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.fileProperties
import kotlin.system.exitProcess

fun main() {

    // check required env vras
    val requiredEnvVars = listOf("DYDX_API_KEY", "DYDX_API_PASSPHRASE", "DYDX_API_SECRET", "ETHEREUM_ADDRESS", "STARK_PRIVATE_KEY")
    val missingEnvVars = requiredEnvVars.filter { System.getenv(it).isNullOrBlank() }
    if (missingEnvVars.isNotEmpty()) {
        val missingVarsMessage = "Missing environment variables: ${missingEnvVars.joinToString()}"
        System.err.println(missingVarsMessage) // print error message to standard error
        exitProcess(1) // exit application with non-zero exit code
    }

    //
    startKoin{
        printLogger(Level.INFO)
        fileProperties()

        // modules
        modules(core)
        modules(dydxExchangeService)
    }

    //
    runBlocking { Kaiex().start() }
}
