/* Copyright 2019 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.common

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.components.compose.ComposeActivity.QueuedMedia
import com.keylesspalace.tusky.components.compose.ComposeAutoCompleteAdapter
import com.keylesspalace.tusky.components.instance.data.models.entity.InstanceEntity
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.components.search.SearchType
import com.keylesspalace.tusky.core.extensions.cancelIfActive
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.entity.StickerPack
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.PostFormat
import com.keylesspalace.tusky.util.RxAwareViewModel
import com.keylesspalace.tusky.util.VersionUtils
import com.keylesspalace.tusky.util.map
import com.keylesspalace.tusky.util.withoutFirstWhich
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Response

open class CommonComposeViewModel(
    private val api: MastodonApi,
    private val mediaUploader: MediaUploader,
    private val instanceRepository: InstanceRepository,
) : RxAwareViewModel() {

    private var job: Job? = null

    protected val instance: MutableLiveData<InstanceEntity?> = MutableLiveData(null)
    protected val stickers: MutableLiveData<Array<StickerPack>> = MutableLiveData(emptyArray())
    val haveStickers: MutableLiveData<Boolean> = MutableLiveData(false)
    var tryFetchStickers = false
    var anonymizeNames = true
    var hasNoAttachmentLimits = false

    val instanceParams: LiveData<ComposeInstanceParams> = instance.map { instance ->
        ComposeInstanceParams(
            maxChars = instance?.maximumTootCharacters ?: InstanceConstants.DEFAULT_CHARACTER_LIMIT,
            chatLimit = instance?.chatLimit ?: InstanceConstants.DEFAULT_CHARACTER_LIMIT,
            pollMaxOptions = instance?.maxPollOptions ?: DEFAULT_MAX_OPTION_COUNT,
            pollMaxLength = instance?.maxPollOptionLength ?: DEFAULT_MAX_OPTION_LENGTH,
            supportsScheduled = instance?.version?.let {
                VersionUtils(it).supportsScheduledToots()
            } ?: false,
            maxMediaAttachments = instance?.maxMediaAttachments ?: InstanceConstants.DEFAULT_STATUS_MEDIA_ITEMS,
            postFormats = instance?.postFormats ?: emptyList()
        )
    }

    val instanceStickers: LiveData<Array<StickerPack>> =
        stickers // .map { stickers -> HashMap<String,String>(stickers) }

    val emoji: MutableLiveData<List<Emoji>?> = MutableLiveData()

    val media = mutableLiveData<List<QueuedMedia>>(listOf())
    val uploadError = MutableLiveData<Throwable>()

    protected val mediaToDisposable = mutableMapOf<Long, Disposable>()

    init {
        getInstanceConfig()
    }

    private fun getInstanceConfig() {
        job?.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            val instanceInfo = getInstanceInfo()
            emoji.postValue(instanceInfo.emojiList)
            instance.postValue(instanceInfo)
        }
    }

    suspend fun getInstanceInfo(): InstanceEntity {
        return instanceRepository.getInstanceInfo().last().asRight()
    }

    fun pickMedia(uri: Uri, filename: String?): LiveData<Either<Throwable, QueuedMedia>> {
        // We are not calling .toLiveData() here because we don't want to stop the process when
        // the Activity goes away temporarily (like on screen rotation).

        // Needed to get the instance config before uploading anything
        // TODO: nasty hack, improve
        if (instance.value == null) {
            instance.value = runBlocking {
                async {
                    getInstanceInfo()
                }.await()
            }
        }

        val liveData = MutableLiveData<Either<Throwable, QueuedMedia>>()
        val imageLimit = instance.value?.imageSizeLimit ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE
        val videoLimit = instance.value?.videoSizeLimit ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE
        val audioLimit = instance.value?.videoSizeLimit ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE

        mediaUploader.prepareMedia(uri, videoLimit, imageLimit, audioLimit, filename)
            .map { (type, uri, size) ->
                val mediaItems = media.value!!
                if (!hasNoAttachmentLimits &&
                    type != QueuedMedia.Type.IMAGE &&
                    mediaItems.isNotEmpty() &&
                    mediaItems[0].type == QueuedMedia.Type.IMAGE
                ) {
                    throw VideoOrImageException()
                } else {
                    addMediaToQueue(
                        type,
                        uri,
                        size,
                        filename ?: "unknown",
                        anonymizeNames
                    )
                }
            }
            .subscribe({ queuedMedia ->
                liveData.postValue(Either.Right(queuedMedia))
            }, { error ->
                liveData.postValue(Either.Left(error))
            })
            .autoDispose()
        return liveData
    }

    private fun addMediaToQueue(
        type: Int,
        uri: Uri,
        mediaSize: Long,
        filename: String,
        anonymizeNames: Boolean
    ): QueuedMedia {
        val mediaItem = QueuedMedia(
            System.currentTimeMillis(),
            uri,
            type,
            mediaSize,
            filename,
            hasNoAttachmentLimits,
            anonymizeNames
        )
        val imageLimit = instance.value?.imageSizeLimit ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE
        val videoLimit = instance.value?.videoSizeLimit ?: InstanceConstants.DEFAULT_STATUS_MEDIA_SIZE

        media.value = media.value!! + mediaItem
        mediaToDisposable[mediaItem.localId] = mediaUploader
            .uploadMedia(mediaItem, videoLimit, imageLimit)
            .subscribe({ event ->
                val item = media.value?.find { it.localId == mediaItem.localId }
                    ?: return@subscribe
                val newMediaItem = when (event) {
                    is UploadEvent.ProgressEvent ->
                        item.copy(uploadPercent = event.percentage)
                    is UploadEvent.FinishedEvent ->
                        item.copy(id = event.attachment.id, uploadPercent = -1)
                }
                synchronized(media) {
                    val mediaValue = media.value!!
                    val index = mediaValue.indexOfFirst { it.localId == newMediaItem.localId }
                    media.postValue(
                        if (index == -1) {
                            mediaValue + newMediaItem
                        } else {
                            mediaValue.toMutableList().also { it[index] = newMediaItem }
                        }
                    )
                }
            }, { error ->
                media.postValue(
                    media.value?.filter { it.localId != mediaItem.localId }
                        ?: emptyList()
                )
                uploadError.postValue(error)
            })
        return mediaItem
    }

    protected fun addUploadedMedia(id: String, type: Int, uri: Uri, description: String?) {
        val mediaItem = QueuedMedia(
            System.currentTimeMillis(), uri, type, 0, "unknown",
            hasNoAttachmentLimits, anonymizeNames, -1, id, description
        )
        media.value = media.value!! + mediaItem
    }

    fun removeMediaFromQueue(item: QueuedMedia) {
        mediaToDisposable[item.localId]?.dispose()
        media.value = media.value!!.withoutFirstWhich { it.localId == item.localId }
    }

    fun updateDescription(localId: Long, description: String): LiveData<Boolean> {
        val newList = media.value!!.toMutableList()
        val index = newList.indexOfFirst { it.localId == localId }
        if (index != -1) {
            newList[index] = newList[index].copy(description = description)
        }
        media.value = newList
        val completedCaptioningLiveData = MutableLiveData<Boolean>()
        media.observeForever(object : Observer<List<QueuedMedia>> {
            override fun onChanged(mediaItems: List<QueuedMedia>) {
                val updatedItem = mediaItems.find { it.localId == localId }
                if (updatedItem == null) {
                    media.removeObserver(this)
                } else if (updatedItem.id != null) {
                    api.updateMedia(updatedItem.id, description)
                        .subscribe({
                            completedCaptioningLiveData.postValue(true)
                        }, {
                            completedCaptioningLiveData.postValue(false)
                        })
                        .autoDispose()
                    media.removeObserver(this)
                }
            }
        })
        return completedCaptioningLiveData
    }

    fun searchAutocompleteSuggestions(token: String):
        List<ComposeAutoCompleteAdapter.AutocompleteResult> {
        when (token[0]) {
            '@' -> {
                return try {
                    val acct = token.substring(1)
                    api.searchAccounts(query = acct, resolve = true, limit = 10)
                        .blockingGet()
                        .map { ComposeAutoCompleteAdapter.AccountResult(it) }
                        .filter {
                            it.account.username.startsWith(acct, ignoreCase = true)
                        }
                } catch (e: Throwable) {
                    Log.e(TAG, String.format("Autocomplete search for %s failed.", token), e)
                    emptyList()
                }
            }
            '#' -> {
                return try {
                    api.searchObservable(
                        query = token,
                        type = SearchType.Hashtag.apiParameter,
                        limit = 10
                    )
                        .blockingGet()
                        .hashtags
                        .map { ComposeAutoCompleteAdapter.HashtagResult(it) }
                } catch (e: Throwable) {
                    Log.e(TAG, String.format("Autocomplete search for %s failed.", token), e)
                    emptyList()
                }
            }
            ':' -> {
                val emojiList = emoji.value ?: return emptyList()

                val incomplete = token.substring(1).lowercase(Locale.ROOT)
                val results = ArrayList<ComposeAutoCompleteAdapter.AutocompleteResult>()
                val resultsInside = ArrayList<ComposeAutoCompleteAdapter.AutocompleteResult>()
                for (emoji in emojiList) {
                    val shortcode = emoji.shortcode.lowercase(Locale.ROOT)
                    if (shortcode.startsWith(incomplete)) {
                        results.add(ComposeAutoCompleteAdapter.EmojiResult(emoji))
                    } else if (shortcode.indexOf(incomplete, 1) != -1) {
                        resultsInside.add(ComposeAutoCompleteAdapter.EmojiResult(emoji))
                    }
                }
                if (results.isNotEmpty() && resultsInside.isNotEmpty()) {
                    results.add(ComposeAutoCompleteAdapter.ResultSeparator())
                }
                results.addAll(resultsInside)
                return results
            }
            else -> {
                Log.w(TAG, "Unexpected autocompletion token: $token")
                return emptyList()
            }
        }
    }

    override fun onCleared() {
        for (uploadDisposable in mediaToDisposable.values) {
            uploadDisposable.dispose()
        }
        super.onCleared()
    }

    private fun getStickers() {
        if (!tryFetchStickers) {
            return
        }

        api.getStickers().subscribe({ stickers ->
            if (stickers.isNotEmpty()) {
                haveStickers.postValue(true)

                val singles = mutableListOf<Single<Response<StickerPack>>>()

                for (entry in stickers) {
                    val url = entry.value.removePrefix("/")
                        .removeSuffix("/") + "/pack.json"
                    singles += api.getStickerPack(url)
                }

                Single.zip(singles) {
                    it.map {
                        it as Response<StickerPack>
                        it.body()!!.internal_url =
                            it.raw().request.url.toString().removeSuffix("pack.json")
                        it.body()!!
                    }
                }.onErrorReturn {
                    Log.d(TAG, "Failed to get sticker pack.json", it)
                    emptyList()
                }.subscribe() { pack ->
                    if (pack.isNotEmpty()) {
                        val array = pack.toTypedArray()
                        array.sort()
                        this.stickers.postValue(array)
                    }
                }.autoDispose()
            }
        }, { err ->
            Log.d(TAG, "Failed to get sticker.json", err)
        }).autoDispose()
    }

    fun setup() {
        getStickers() // early as possible
    }

    private companion object {
        const val TAG = "CCVM"
    }
}

fun <T> mutableLiveData(default: T) = MutableLiveData<T>().apply { value = default }

const val DEFAULT_MAX_OPTION_COUNT = 4
const val DEFAULT_MAX_OPTION_LENGTH = 25

data class ComposeInstanceParams(
    val maxChars: Int,
    val chatLimit: Int,
    val pollMaxOptions: Int,
    val pollMaxLength: Int,
    val supportsScheduled: Boolean,
    val maxMediaAttachments: Int,
    val postFormats: List<PostFormat>
)

/**
 * Throw when trying to add an image when video is already present or the other way around
 */
class VideoOrImageException : Exception()
