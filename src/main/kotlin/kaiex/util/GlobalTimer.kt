package kaiex.util

import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object GlobalTimer {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

//    fun scheduleAtFixedRate(delay: Long, interval: Long, task: () -> Unit) {
//        executor.scheduleAtFixedRate(task, delay, interval, TimeUnit.MINUTES)
//    }

    fun scheduleAtFixedTime(interval: Long, task: () -> Unit) {
        val initialDelay = getNextMinuteBoundary() - System.currentTimeMillis()
        executor.scheduleAtFixedRate(task, initialDelay, interval, TimeUnit.MILLISECONDS)
    }

    fun shutdown() {
        executor.shutdown()
    }

    private fun getNextMinuteBoundary(): Long {
        val now = System.currentTimeMillis()
        val nextMinute = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return nextMinute
    }
}