package com.keylesspalace.tusky.components.instance

import com.keylesspalace.tusky.core.extensions.notNull
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InstanceRepository(
    private val accountManager: AccountManager,
    private val service: MastodonService,
    private val db: AppDatabase
) {

    fun getInstanceInfo(): Flow<Either<Nothing, InstanceEntity>> = flow {
        service.getInstanceData().run {
            val response = if (isSuccessful && body().notNull()) {
                val instance = body()!!
                InstanceEntity(
                    instance = accountManager.activeAccount?.domain!!,
                    emojiList = null,
                    maximumTootCharacters = instance.maxTootChars,
                    maxPollOptions = instance.pollLimits?.maxOptions,
                    maxPollOptionLength = instance.pollLimits?.maxOptionChars,
                    maxBioLength = instance.descriptionLimit,
                    maxBioFields = instance.pleroma?.metadata?.fieldsLimits?.maxFields,
                    version = instance.version,
                    chatLimit = instance.chatLimit
                )
            } else {
                getInstanceInfoDb()
            }

            emit(Either.Right(response))
        }
    }

    fun getInstanceInfoDb(): InstanceEntity {
        return db.instanceDao().loadFromCache(accountManager.activeAccount!!.domain)
    }
}
