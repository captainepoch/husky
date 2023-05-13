/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.components.instance.domain.repository

import com.keylesspalace.tusky.components.instance.data.models.entity.InstanceEntity
import com.keylesspalace.tusky.core.extensions.notNull
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class InstanceRepositoryImp(
    private val accountManager: AccountManager,
    private val service: MastodonService,
    private val db: AppDatabase
) : InstanceRepository {

    override fun getInstanceInfo(): Flow<Either<Nothing, InstanceEntity>> = flow {
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

    override fun getInstanceInfoDb(): InstanceEntity {
        return db.instanceDao().loadFromCache(accountManager.activeAccount!!.domain)
    }
}
