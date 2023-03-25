package kaiex.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull

class EventBroadcaster<T> {
    private val eventFlow = MutableSharedFlow<T?>()

    suspend fun sendEvent(event: T) {
        eventFlow.emit(event)
    }

    fun listenForEvents(): Flow<T> {
        return eventFlow.filterNotNull()
    }
}