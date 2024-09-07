/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2024  The Husky Developers
 * Copyright (C) 2020  Tusky Contributors
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

package com.keylesspalace.tusky.components.announcements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.keylesspalace.tusky.appstore.AnnouncementReadEvent
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.components.instance.domain.repository.InstanceRepository
import com.keylesspalace.tusky.core.extensions.cancelIfActive
import com.keylesspalace.tusky.entity.Announcement
import com.keylesspalace.tusky.entity.Emoji
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.Error
import com.keylesspalace.tusky.util.Loading
import com.keylesspalace.tusky.util.Resource
import com.keylesspalace.tusky.util.RxAwareViewModel
import com.keylesspalace.tusky.util.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class AnnouncementsViewModel(
    private val mastodonApi: MastodonApi,
    private val instanceRepository: InstanceRepository,
    private val eventHub: EventHub
) : RxAwareViewModel() {

    private var job: Job? = null

    private val announcementsMutable = MutableLiveData<Resource<List<Announcement>>>()
    val announcements: LiveData<Resource<List<Announcement>>>
        get() = announcementsMutable

    private val emojisMutable = MutableLiveData<List<Emoji>?>()
    val emojis: LiveData<List<Emoji>?>
        get() = emojisMutable

    init {
        getEmojis()
    }

    private fun getEmojis() {
        job?.cancelIfActive()
        job = viewModelScope.launch(Dispatchers.IO) {
            emojisMutable.postValue(instanceRepository.getEmojis())
        }
    }

    fun load() {
        announcementsMutable.postValue(Loading())
        mastodonApi.listAnnouncements()
            .subscribe({
                announcementsMutable.postValue(Success(it))
                it.filter { announcement -> !announcement.read }
                    .forEach { announcement ->
                        mastodonApi.dismissAnnouncement(announcement.id)
                            .subscribe(
                                {
                                    eventHub.dispatch(AnnouncementReadEvent(announcement.id))
                                },
                                { throwable ->
                                    Timber.e(throwable, "Failed to mark announcement as read.")
                                }
                            )
                            .autoDispose()
                    }
            }, {
                announcementsMutable.postValue(Error(cause = it))
            })
            .autoDispose()
    }

    fun addReaction(announcementId: String, name: String) {
        mastodonApi.addAnnouncementReaction(announcementId, name)
            .subscribe({
                announcementsMutable.postValue(
                    Success(
                        announcements.value!!.data!!.map { announcement ->
                            if (announcement.id == announcementId) {
                                announcement.copy(
                                    reactions = if (announcement.reactions.find { reaction -> reaction.name == name } != null) {
                                        announcement.reactions.map { reaction ->
                                            if (reaction.name == name) {
                                                reaction.copy(
                                                    count = reaction.count + 1,
                                                    me = true
                                                )
                                            } else {
                                                reaction
                                            }
                                        }
                                    } else {
                                        listOf(
                                            *announcement.reactions.toTypedArray(),
                                            emojis.value!!.find { emoji -> emoji.shortcode == name }!!
                                                .run {
                                                    Announcement.Reaction(
                                                        name,
                                                        1,
                                                        true,
                                                        url,
                                                        staticUrl
                                                    )
                                                }
                                        )
                                    }
                                )
                            } else {
                                announcement
                            }
                        }
                    )
                )
            }, {
                Timber.w("Failed to add reaction to the announcement.", it)
            })
            .autoDispose()
    }

    fun removeReaction(announcementId: String, name: String) {
        mastodonApi.removeAnnouncementReaction(announcementId, name)
            .subscribe({
                announcementsMutable.postValue(
                    Success(
                        announcements.value!!.data!!.map { announcement ->
                            if (announcement.id == announcementId) {
                                announcement.copy(
                                    reactions = announcement.reactions.mapNotNull { reaction ->
                                        if (reaction.name == name) {
                                            if (reaction.count > 1) {
                                                reaction.copy(
                                                    count = reaction.count - 1,
                                                    me = false
                                                )
                                            } else {
                                                null
                                            }
                                        } else {
                                            reaction
                                        }
                                    }
                                )
                            } else {
                                announcement
                            }
                        }
                    )
                )
            }, {
                Timber.w("Failed to remove reaction from the announcement.", it)
            })
            .autoDispose()
    }
}
