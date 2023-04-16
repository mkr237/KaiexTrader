package kaiex.model

import kotlinx.coroutines.flow.Flow

//interface AccountInfo

data class AccountUpdate(
    val id:String,
    val orders:List<OrderUpdate>,
    val fills:List<OrderFill>,
    val positions:List<Position>
)

interface AccountService {
    suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate>
    suspend fun unsubscribeAccountUpdate(): Flow<AccountUpdate>
}
