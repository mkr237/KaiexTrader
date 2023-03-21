import com.kaiex.Kaiex
import com.kaiex.dydxExchangeService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.fileProperties

fun main() {
    startKoin{
        printLogger(Level.DEBUG)
        fileProperties()

        // modules
        modules(dydxExchangeService)
    }

    runBlocking {
        Kaiex().start()
    }
}
