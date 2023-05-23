package kaiex.strategy

interface KaiexStrategy {
    suspend fun start()
    suspend fun stop()
}