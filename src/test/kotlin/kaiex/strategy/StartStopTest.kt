package kaiex.strategy

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import java.time.Instant

fun main() {
    runBlocking {
        start()
        stop()
    }
}

fun start() {
    println("Start @ ${Instant.now()}")
    job1()
    job2()
    println("FINISHED @ ${Instant.now()}")
}

fun job1() {
    CoroutineScope(Dispatchers.Default).launch {
        flow {
            repeat(100) {
                emit("Job1 Event $it")
            }
        }.collect {
            println(it)
        }
    }
}

fun job2() {
    CoroutineScope(Dispatchers.Default).launch {
        flow {
            repeat(100) {
                emit("Job2 Event $it")
            }
        }.collect {
            println(it)
        }
    }
}

fun stop() {
    println("Stop @ ${Instant.now()}")
}