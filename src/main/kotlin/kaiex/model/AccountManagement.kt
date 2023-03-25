package kaiex.model

import kotlinx.coroutines.flow.Flow

interface AccountInfo

data class AccountSnapshot(val id:String)

data class AccountUpdate(val id:String)

interface AccountService {
    suspend fun subscribeAccountUpdates(): Flow<AccountSnapshot>
    suspend fun unsubscribeAccountUpdate(): Flow<AccountSnapshot>
}
