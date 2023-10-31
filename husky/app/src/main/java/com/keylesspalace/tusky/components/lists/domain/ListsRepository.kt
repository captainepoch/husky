package com.keylesspalace.tusky.components.lists.domain

import com.keylesspalace.tusky.components.lists.account.model.ListForAccountError
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.entity.MastoList
import kotlinx.coroutines.flow.Flow

interface ListsRepository {

    suspend fun getLists(): Flow<Either<ListForAccountError, List<MastoList>>>

    suspend fun getListsIncludesAccount(
        userAccountId: String
    ): Flow<Either<ListForAccountError, List<MastoList>>>

    suspend fun addAccountToList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<ListForAccountError, Unit>>

    suspend fun removeAccountFromList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<ListForAccountError, Unit>>
}
