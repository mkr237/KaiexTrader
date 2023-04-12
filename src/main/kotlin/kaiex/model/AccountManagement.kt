package kaiex.model

import kotlinx.coroutines.flow.Flow

//interface AccountInfo

data class AccountUpdate(val id:String)

interface AccountService {
    suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate>
    suspend fun unsubscribeAccountUpdate(): Flow<AccountUpdate>
}
