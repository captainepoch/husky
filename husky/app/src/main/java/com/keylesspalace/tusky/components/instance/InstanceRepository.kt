package com.keylesspalace.tusky.components.instance

import com.keylesspalace.tusky.core.extensions.notNull
import com.keylesspalace.tusky.core.network.ApiResponse
import com.keylesspalace.tusky.core.network.ApiResponse.Success
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

    fun getInstanceInfo(): Flow<ApiResponse<InstanceEntity>> = flow {
        service.getInstanceData().run {
            val response = if (isSuccessful && body().notNull()) {
                val instance = body()!!
                Success(
                    InstanceEntity(
                        instance = accountManager.activeAccount?.domain!!,
                        emojiList = null,
                        maximumTootCharacters = instance.maxTootChars,
                        maxPollOptions = instance.pollLimits?.maxOptions,
                        maxPollOptionLength = instance.pollLimits?.maxOptionChars,
                        maxBioLength = instance.descriptionLimit,
                        version = instance.version,
                        chatLimit = instance.chatLimit
                    )
                )
            } else {
                Success(getInstanceInfoDb())
            }

            emit(response)
        }
    }

    fun getInstanceInfoDb(): InstanceEntity {
        return db.instanceDao().loadFromCache(accountManager.activeAccount!!.domain)
    }
}
