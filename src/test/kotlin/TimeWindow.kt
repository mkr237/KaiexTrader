import kaiex.util.timedChunk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() = runBlocking<Unit> {
    val windowSize = 10_000L // 10 seconds
    val interval = 60_000L // 1 minute
    var count = 0 // number of events received in the last minute
    var lastMinute = LocalDateTime.now().withSecond(0).withNano(0) // the last minute for which the count was printed
    val flow = flow {
        while (true) {
            emit((1..10).random()) // simulate a random stream of integers
            delay((500..2000).random().toLong()) // simulate random delays between emissions
        }
    }

    flow
        .timedChunk(10000L)
        .onEach { e ->
            println(e)
        }
        .launchIn(this)

    delay(Long.MAX_VALUE) // wait indefinitely
}
