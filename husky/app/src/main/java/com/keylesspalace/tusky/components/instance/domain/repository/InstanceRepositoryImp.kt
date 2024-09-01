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

import com.keylesspalace.tusky.components.instance.data.models.InstanceFeatures
import com.keylesspalace.tusky.components.instance.data.models.InstanceFeatures.QUOTE_POSTING
import com.keylesspalace.tusky.components.instance.data.models.data.Instance
import com.keylesspalace.tusky.components.instance.data.models.entity.InstanceEntity
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonService
import com.keylesspalace.tusky.util.PostFormat
import com.keylesspalace.tusky.util.PostFormat.Companion
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class InstanceRepositoryImp(
    private val accountManager: AccountManager,
    private val service: MastodonService,
    private val db: AppDatabase
) : InstanceRepository {

    override fun getInstanceInfo(): Flow<Either<Nothing, InstanceEntity>> = flow {
        service.getInstanceData().run {
            val instanceBody = body()
            val instance = if (isSuccessful && instanceBody != null) {
                parseInstance(instanceBody)
            } else {
                getInstanceInfoDb()
            }

            // TODO: get CustomEmojis before saving the instance
            //db.instanceDao().insertOrReplace(instance)
            emit(Either.Right(instance))
        }
    }

    override fun getInstanceInfoRx(): Single<InstanceEntity> {
        return service.getInstance().map {
            parseInstance(it)
        }.onErrorReturn {
            getInstanceInfoDb()
        }
    }

    private fun parseInstance(instanceRemote: Instance): InstanceEntity {
        val features: List<InstanceFeatures> =
            instanceRemote.pleroma?.metadata?.features?.mapNotNull {
                InstanceFeatures.getInstanceFeature(it)
            } ?: listOf()

        val postFormats = (instanceRemote.pleroma?.metadata?.postsFormats
            ?: instanceRemote.mastodonConfig?.statuses?.postFormats) ?: emptyList()

        return InstanceEntity(instance = accountManager.activeAccount?.domain!!,
            emojiList = null,
            maximumTootCharacters = instanceRemote.maxTootChars,
            maxPollOptions = instanceRemote.pollLimits?.maxOptions,
            maxPollOptionLength = instanceRemote.pollLimits?.maxOptionChars,
            maxBioLength = instanceRemote.descriptionLimit,
            maxBioFields = instanceRemote.pleroma?.metadata?.fieldsLimits?.maxFields,
            version = instanceRemote.version,
            chatLimit = instanceRemote.chatLimit,
            quotePosting = features.contains(QUOTE_POSTING),
            maxMediaAttachments = instanceRemote.maxMediaAttachments
                ?: (instanceRemote.mastodonConfig?.statuses?.maxMediaAttachments
                    ?: InstanceConstants.DEFAULT_STATUS_MEDIA_ITEMS),
            imageSizeLimit = (instanceRemote.uploadLimit
                ?: instanceRemote.mastodonConfig?.mediaAttachments?.imageSizeLimit)
                ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE,
            videoSizeLimit = (instanceRemote.uploadLimit
                ?: instanceRemote.mastodonConfig?.mediaAttachments?.videoSizeLimit)
                ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE,
            postFormats = postFormats.map { PostFormat.getFormat(it) })
    }

    override fun getInstanceInfoDb(): InstanceEntity {
        return db.instanceDao().loadFromCache(accountManager.activeAccount!!.domain)
    }
}
