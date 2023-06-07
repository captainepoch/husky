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

package com.keylesspalace.tusky.components.unifiedpush

import android.content.Context
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Marker
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.isLessThan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationFetcher(
    private val api: MastodonApi,
    private val accountManager: AccountManager,
    private val context: Context
) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun fetchAndShow() {
        scope.launch {
            val notifications = fetchNotifications(accountManager.activeAccount!!)
            notifications.forEachIndexed { index, notification ->
                NotificationHelper.make(
                    context,
                    notification,
                    accountManager.activeAccount,
                    index == 0
                )
                Timber.d("Notification $index made")

                // Delay pushing notifications too fast because of Android limits
                delay(500)
            }
        }
    }

    private suspend fun fetchNotifications(account: AccountEntity): MutableList<Notification> {
        val token = "Bearer ${account.accessToken}"

        // Fetch markers to not load/show notifications the user already saw
        val marker = fetchMarkers(token, account.domain)
        if (marker != null && account.lastNotificationId.isLessThan(marker.lastReadId)) {
            account.lastNotificationId = marker.lastReadId
        }
        Timber.d("Getting Notifications for ${account.username}")
        val notificationsResponse = api.notificationsWithAuthCoroutine(
            token,
            account.domain,
            account.lastNotificationId
        )

        val newId = account.lastNotificationId
        val notifications = mutableListOf<Notification>()
        var newestId = ""
        if (notificationsResponse.body() != null) {
            Timber.d("Notifications not null")
            notificationsResponse.body()!!.reversed().forEach { notification ->
                val currentId = notification.id
                if (newestId.isLessThan(currentId)) {
                    newestId = currentId
                    account.lastNotificationId = currentId
                }

                if (newId.isLessThan(currentId)) {
                    Timber.d("Notification added")
                    notifications.add(notification)
                }
            }
        }

        return notifications
    }

    private suspend fun fetchMarkers(token: String, domain: String): Marker? {
        val markersResponse = api.markersWithAuthCoroutine(token, domain, listOf("notifications"))
        if (markersResponse.body() != null) {
            val markersMap = markersResponse.body()!!
            val notificationMarker = markersMap["notifications"]
            Timber.d("Fetched markers [$notificationMarker]")

            return notificationMarker
        }

        Timber.e("Error [${markersResponse.raw()}]")

        return null
    }
}
