package kaiex.util

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object GlobalTimer {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    fun scheduleAtFixedRate(delay: Long, interval: Long, task: () -> Unit) {
        executor.scheduleAtFixedRate(task, delay, interval, TimeUnit.MILLISECONDS)
    }

    fun shutdown() {
        executor.shutdown()
    }
}

//GlobalTimer.scheduleAtFixedRate(0, 1000) {
//    println("Hello, world!")
//}
