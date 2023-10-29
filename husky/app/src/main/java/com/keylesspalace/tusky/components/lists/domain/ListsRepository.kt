package com.keylesspalace.tusky.components.lists.domain

import com.keylesspalace.tusky.core.functional.CustomError
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.entity.MastoList
import kotlinx.coroutines.flow.Flow

interface ListsRepository {

    suspend fun getLists(): Flow<Either<CustomError, List<MastoList>>>

    suspend fun getListsIncludesAccount(
        userAccountId: String
    ): Flow<Either<CustomError, List<MastoList>>>

    suspend fun addAccountToList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<CustomError, Unit>>

    suspend fun removeAccountFromList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<CustomError, Unit>>
}
