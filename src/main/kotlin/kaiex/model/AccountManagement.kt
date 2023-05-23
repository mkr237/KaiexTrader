package kaiex.model

import kotlinx.coroutines.flow.Flow

data class AccountUpdate(
    val id:String,
    // TODO what about the other fields?
    val orders:List<OrderUpdate>,
    val fills:List<OrderFill>,
    val positions:List<Position>
)

interface AccountService {
    suspend fun subscribeAccountUpdates(accountId: String): Flow<AccountUpdate>
}
