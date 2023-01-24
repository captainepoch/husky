package com.keylesspalace.tusky.components.instance

import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonService
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import timber.log.Timber

class InstanceRepository(
    private val accountManager: AccountManager,
    private val service: MastodonService,
    private val db: AppDatabase
) {

    fun getInstanceInfo(disposables: CompositeDisposable, callback: (InstanceInfo) -> Unit) {
        service.getInstance().subscribe(
            { instance ->
                val entity = InstanceEntity(
                    instance = accountManager.activeAccount?.domain!!,
                    emojiList = null,
                    maximumTootCharacters = instance.maxTootChars,
                    maxPollOptions = instance.pollLimits?.maxOptions,
                    maxPollOptionLength = instance.pollLimits?.maxOptionChars,
                    maxBioLength = instance.descriptionLimit,
                    version = instance.version,
                    chatLimit = instance.chatLimit
                )
                db.instanceDao().insertOrReplace(entity)

                mapToInstanceInfo(entity, callback)

                Timber.d("Instance information retrieved correctly")
            },
            {
                mapToInstanceInfo(
                    db.instanceDao().loadFromCache(accountManager.activeAccount!!.domain),
                    callback
                )

                Timber.e("Fail to retrieve instance information, fallback to cache")
            }
        ).addTo(disposables)
    }

    private fun mapToInstanceInfo(entity: InstanceEntity, callback: (InstanceInfo) -> Unit) {
        val instanceInfo = InstanceInfo(
            maxBioLength = entity.maxBioLength ?: InstanceConstants.DEFAULT_BIO_LENGTH,
            maxTootLength = entity.maximumTootCharacters ?: InstanceConstants.DEFAULT_CHARACTER_LIMIT
        )

        callback(instanceInfo)
    }
}
