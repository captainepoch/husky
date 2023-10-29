package com.keylesspalace.tusky.components.lists.domain

import com.keylesspalace.tusky.core.functional.CustomError
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.core.functional.Either.Left
import com.keylesspalace.tusky.core.functional.Either.Right
import com.keylesspalace.tusky.entity.MastoList
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class ListsRepositoryImpl(
    private val service: MastodonApi
) : ListsRepository {

    override suspend fun getLists(): Flow<Either<CustomError, List<MastoList>>> = flow {
        service.coGetLists().run {
            val body = body()
            if (isSuccessful && body != null) {
                Timber.d("Body: [$body]")

                emit(Right(body))
            } else {
                Timber.e("Error: ${errorBody()?.string()}")

                emit(Left(CustomError.GenericError))
            }
        }
    }

    override suspend fun getListsIncludesAccount(userAccountId: String): Flow<Either<CustomError, List<MastoList>>> =
        flow {
            service.getListsIncludesAccount(userAccountId).run {
                val body = body()
                if (isSuccessful && body != null) {
                    Timber.d("Body: [$body]")

                    emit(Right(body))
                } else {
                    Timber.e("Error: ${errorBody()?.string()}")

                    emit(Left(CustomError.GenericError))
                }
            }
        }

    override suspend fun addAccountToList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<CustomError, Unit>> = flow {
        service.coAddAccountToList(listId, accountIds).run {
            val body = body()
            if (isSuccessful && body != null) {
                emit(Right(Unit))
            } else {
                emit(Left(CustomError.GenericError))
            }
        }
    }

    override suspend fun removeAccountFromList(
        listId: String,
        accountIds: List<String>
    ): Flow<Either<CustomError, Unit>> = flow {
        service.coDeleteAccountFromList(listId, accountIds).run {
            val body = body()
            if (isSuccessful && body != null) {
                emit(Right(Unit))
            } else {
                emit(Left(CustomError.GenericError))
            }
        }
    }
}
