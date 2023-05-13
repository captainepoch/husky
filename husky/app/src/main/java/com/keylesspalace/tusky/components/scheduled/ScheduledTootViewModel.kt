/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
 * Copyright (C) 2019  Tusky Contributors
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

package com.keylesspalace.tusky.components.scheduled

import android.util.Log
import androidx.paging.Config
import androidx.paging.toLiveData
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.StatusScheduledEvent
import com.keylesspalace.tusky.entity.ScheduledStatus
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.RxAwareViewModel
import io.reactivex.android.schedulers.AndroidSchedulers

class ScheduledTootViewModel(
    private val mastodonApi: MastodonApi,
    private val eventHub: EventHub
) : RxAwareViewModel() {

    private val dataSourceFactory = ScheduledTootDataSourceFactory(mastodonApi, disposables)

    val data = dataSourceFactory.toLiveData(
        config = Config(pageSize = 20, initialLoadSizeHint = 20, enablePlaceholders = false)
    )

    val networkState = dataSourceFactory.networkState

    init {
        eventHub.events
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                if (event is StatusScheduledEvent) {
                    reload()
                }
            }
            .autoDispose()
    }

    fun reload() {
        dataSourceFactory.reload()
    }

    fun deleteScheduledStatus(status: ScheduledStatus) {
        mastodonApi.deleteScheduledStatus(status.id)
            .subscribe({
                dataSourceFactory.remove(status)
            }, { throwable ->
                Log.w("ScheduledTootViewModel", "Error deleting scheduled status", throwable)
            })
            .autoDispose()
    }
}
