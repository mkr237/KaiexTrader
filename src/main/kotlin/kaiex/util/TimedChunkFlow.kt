package kaiex.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TimedChunkFlow<T>(sourceFlow: Flow<T>, periodMs: Long) {
    private val chunkLock = ReentrantLock()
    private var chunk = mutableListOf<T>()

    val resultFlow = flow {
        sourceFlow.collect {
            val localChunk = chunkLock.withLock {
                println(it)
                chunk.add(it)
                chunk
            }
            emit(localChunk)
        }
    }.sample(periodMs).onEach {
        chunkLock.withLock {
            chunk = mutableListOf()
        }
    }
}

/**
 * Extends Flow with the above
 */
fun <T> Flow<T>.timedChunk(periodMs: Long): Flow<List<T>> = TimedChunkFlow(this, periodMs).resultFlow
