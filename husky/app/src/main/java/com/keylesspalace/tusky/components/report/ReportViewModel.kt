/* Copyright 2019 Joel Pyska
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

package com.keylesspalace.tusky.components.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.keylesspalace.tusky.appstore.BlockEvent
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.MuteEvent
import com.keylesspalace.tusky.components.report.adapter.StatusesRepository
import com.keylesspalace.tusky.components.report.model.StatusViewState
import com.keylesspalace.tusky.entity.Relationship
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.BiListing
import com.keylesspalace.tusky.util.Error
import com.keylesspalace.tusky.util.Loading
import com.keylesspalace.tusky.util.NetworkState
import com.keylesspalace.tusky.util.Resource
import com.keylesspalace.tusky.util.RxAwareViewModel
import com.keylesspalace.tusky.util.Success
import com.keylesspalace.tusky.util.switchMap
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ReportViewModel(
    private val mastodonApi: MastodonApi,
    private val eventHub: EventHub,
    private val statusesRepository: StatusesRepository
) : RxAwareViewModel() {

    private val navigationMutable = MutableLiveData<Screen?>()
    val navigation: LiveData<Screen?>
        get() = navigationMutable

    private val muteStateMutable = MutableLiveData<Resource<Boolean>>()
    val muteState: LiveData<Resource<Boolean>> = muteStateMutable

    private val blockStateMutable = MutableLiveData<Resource<Boolean>>()
    val blockState: LiveData<Resource<Boolean>> = blockStateMutable

    private val reportingStateMutable = MutableLiveData<Resource<Boolean>>()
    var reportingState: LiveData<Resource<Boolean>> = reportingStateMutable

    private val checkUrlMutable = MutableLiveData<String?>()
    val checkUrl: LiveData<String?>
        get() = checkUrlMutable

    private val repoResult = MutableLiveData<BiListing<Status>>()
    val statuses: LiveData<PagedList<Status>> =
        repoResult.switchMap { it.pagedList }
    val networkStateAfter: LiveData<NetworkState> =
        repoResult.switchMap { it.networkStateAfter }
    val networkStateBefore: LiveData<NetworkState> =
        repoResult.switchMap { it.networkStateBefore }
    val networkStateRefresh: LiveData<NetworkState> =
        repoResult.switchMap { it.refreshState }

    private val selectedIds = HashSet<String>()
    val statusViewState = StatusViewState()

    var reportNote: String = ""
    var isRemoteNotify = false

    private var statusId: String? = null
    lateinit var accountUserName: String
    lateinit var accountId: String
    var isRemoteAccount: Boolean = false
    var remoteServer: String? = null

    fun init(accountId: String, userName: String, statusId: String?) {
        this.accountId = accountId
        this.accountUserName = userName
        this.statusId = statusId
        statusId?.let {
            selectedIds.add(it)
        }

        isRemoteAccount = userName.contains('@')
        if (isRemoteAccount) {
            remoteServer = userName.substring(userName.indexOf('@') + 1)
        }

        obtainRelationship()
        repoResult.value = statusesRepository.getStatuses(accountId, statusId, disposables)
    }

    fun navigateTo(screen: Screen) {
        navigationMutable.value = screen
    }

    fun navigated() {
        navigationMutable.value = null
    }

    private fun obtainRelationship() {
        val ids = listOf(accountId)
        muteStateMutable.value = Loading()
        blockStateMutable.value = Loading()
        mastodonApi.relationships(ids)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { data ->
                    updateRelationship(data.getOrNull(0))
                },
                {
                    updateRelationship(null)
                }
            )
            .autoDispose()
    }

    private fun updateRelationship(relationship: Relationship?) {
        if (relationship != null) {
            muteStateMutable.value = Success(relationship.muting)
            blockStateMutable.value = Success(relationship.blocking)
        } else {
            muteStateMutable.value = Error(false)
            blockStateMutable.value = Error(false)
        }
    }

    fun toggleMute() {
        val alreadyMuted = muteStateMutable.value?.data == true
        if (alreadyMuted) {
            mastodonApi.unmuteAccount(accountId)
        } else {
            mastodonApi.muteAccount(accountId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { relationship ->
                    val muting = relationship?.muting == true
                    muteStateMutable.value = Success(muting)
                    if (muting) {
                        eventHub.dispatch(MuteEvent(accountId, true))
                    }
                },
                { error ->
                    muteStateMutable.value = Error(false, error.message)
                }
            ).autoDispose()

        muteStateMutable.value = Loading()
    }

    fun toggleBlock() {
        val alreadyBlocked = blockStateMutable.value?.data == true
        if (alreadyBlocked) {
            mastodonApi.unblockAccount(accountId)
        } else {
            mastodonApi.blockAccount(accountId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { relationship ->
                    val blocking = relationship?.blocking == true
                    blockStateMutable.value = Success(blocking)
                    if (blocking) {
                        eventHub.dispatch(BlockEvent(accountId))
                    }
                },
                { error ->
                    blockStateMutable.value = Error(false, error.message)
                }
            )
            .autoDispose()

        blockStateMutable.value = Loading()
    }

    fun doReport() {
        reportingStateMutable.value = Loading()
        mastodonApi.reportObservable(
            accountId,
            selectedIds.toList(),
            reportNote,
            if (isRemoteAccount) isRemoteNotify else null
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    reportingStateMutable.value = Success(true)
                },
                { error ->
                    reportingStateMutable.value = Error(cause = error)
                }
            )
            .autoDispose()
    }

    fun retryStatusLoad() {
        repoResult.value?.retry?.invoke()
    }

    fun refreshStatuses() {
        repoResult.value?.refresh?.invoke()
    }

    fun checkClickedUrl(url: String?) {
        checkUrlMutable.value = url
    }

    fun urlChecked() {
        checkUrlMutable.value = null
    }

    fun setStatusChecked(status: Status, checked: Boolean) {
        if (checked) {
            selectedIds.add(status.id)
        } else {
            selectedIds.remove(status.id)
        }
    }

    fun isStatusChecked(id: String): Boolean {
        return selectedIds.contains(id)
    }
}
