package com.keylesspalace.tusky.components.lists.domain

import com.keylesspalace.tusky.components.lists.account.model.ListForAccountError
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.ADD
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.DEL
import com.keylesspalace.tusky.components.lists.account.model.ListsForAccountErrorAction.LOAD
import com.keylesspalace.tusky.core.functional.CommonState.NetworkError
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.core.functional.ErrorMapper
import com.keylesspalace.tusky.entity.MastoList
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ListsRepositoryImpl(
    private val service: MastodonApi
) : ListsRepository {

    override suspend fun getLists(): Flow<Either<ListForAccountError, List<MastoList>>> = flow {
        runCatching {
            service.coGetLists()
        }.map { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(Right(body))
            } else {
                emit(Left(ListForAccountError.UnknownError(action = LOAD)))
            }
        }.getOrElse { failure ->
            when (ErrorMapper.networkErrorMapper(failure)) {
                is NetworkError -> emit(Left(ListForAccountError.NetworkError(action = LOAD)))
                else -> Unit
            }
        }
    }

    override suspend fun getListsIncludesAccount(userAccountId: String): Flow<Either<ListForAccountError, List<MastoList>>> =
        flow {
            runCatching {
                service.getListsIncludesAccount(userAccountId)
            }.map { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    emit(Right(body))
                } else {
                    emit(Left(ListForAccountError.UnknownError(action = LOAD)))
                }
            }.getOrElse { failure ->
                when (ErrorMapper.networkErrorMapper(failure)) {
                    is NetworkError -> emit(Left(ListForAccountError.NetworkError(action = LOAD)))
                    else -> Unit
                }
            }
        }

    override suspend fun addAccountToList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<ListForAccountError, Unit>> = flow {
        runCatching {
            service.coAddAccountToList(listId, accountIds)
        }.map { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(Right(body))
            } else {
                emit(Left(ListForAccountError.UnknownError(listId, ADD)))
            }
        }.getOrElse { failure ->
            when (ErrorMapper.networkErrorMapper(failure)) {
                is NetworkError -> emit(Left(ListForAccountError.NetworkError(listId, ADD)))
                else -> Unit
            }
        }
    }

    override suspend fun removeAccountFromList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<ListForAccountError, Unit>> = flow {
        runCatching {
            service.coDeleteAccountFromList(listId, accountIds)
        }.map { response ->
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(Right(body))
            } else {
                emit(Left(ListForAccountError.UnknownError(listId, DEL)))
            }
        }.getOrElse { failure ->
            when (ErrorMapper.networkErrorMapper(failure)) {
                is NetworkError -> emit(Left(ListForAccountError.NetworkError(listId, DEL)))
                else -> Unit
            }
        }
    }
}
