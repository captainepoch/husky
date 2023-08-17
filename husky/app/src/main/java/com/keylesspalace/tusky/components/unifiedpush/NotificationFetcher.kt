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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.core.extensions.Empty
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.isLessThan
import kotlinx.coroutines.delay
import timber.log.Timber

class NotificationFetcher(
    private val api: MastodonApi,
    private val accountManager: AccountManager,
    private val context: Context
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @SuppressLint("MissingPermission")
    suspend fun fetchAndShow(instance: String) {
        accountManager.getAccountByUnifiedPushInstance(instance)?.let { account ->
            fetchNotifications(account).forEachIndexed { index, notification ->
                val androidNotification = NotificationHelper.make(
                    context,
                    notificationManager,
                    notification,
                    account,
                    index == 0
                )

                Timber.d("Notification $index made")

                androidNotification?.let { pushNotification ->
                    notificationManager.notify(
                        notification.id,
                        account.id.toInt(),
                        pushNotification
                    )
                }

                // Delay pushing notifications too fast because of Android limit
                delay(500)
            }
        }
    }

    private suspend fun fetchNotifications(account: AccountEntity): MutableList<Notification> {
        val token = "Bearer ${account.accessToken}"

        val lastRemoteMarker = fetchLastMarkerRemoteId(token, account.domain)
        val lastReadNotificationId = account.lastNotificationId
        val lastMarkerNotificationId = account.lastNotificationMarkerId
        val minMarkerId = minOf(lastRemoteMarker, lastReadNotificationId, lastMarkerNotificationId)

        Timber.d(
            "lastRemoteMarker[$lastRemoteMarker] " +
                "lastReadNotificationId[$lastReadNotificationId] " +
                "lastMarkerNotificationId[$lastMarkerNotificationId] " +
                "minMarkerId[$minMarkerId]"
        )

        Timber.d("Getting Notifications for ${account.username} from id $minMarkerId")
        val notifications = mutableListOf<Notification>()
        val notificationsResponse = api.notificationsWithAuthCoroutine(
            token,
            account.domain,
            minMarkerId
        )

        if (notificationsResponse.isSuccessful && notificationsResponse.body() != null) {
            var newMarkerId = String.Empty
            Timber.d("Notifications not null")
            notificationsResponse.body()!!.reversed().forEach { notification ->
                val currentId = notification.id
                if (newMarkerId.isLessThan(currentId)) {
                    newMarkerId = currentId
                    account.lastNotificationId = currentId
                }

                if (lastReadNotificationId.isLessThan(currentId)) {
                    Timber.d("Notification added")
                    notifications.add(notification)
                }
            }
        }

        return notifications
    }

    private suspend fun fetchLastMarkerRemoteId(token: String, domain: String): String {
        val markersResponse = api.markersWithAuthCoroutine(token, domain, listOf("notifications"))
        if (markersResponse.body() != null) {
            val markersMap = markersResponse.body()!!
            val notificationMarker = markersMap["notifications"]
            Timber.d("Fetched markers [$notificationMarker]")

            return notificationMarker?.lastReadId ?: "0"
        }

        Timber.e("Error [${markersResponse.raw()}]")

        return "0"
    }
}
