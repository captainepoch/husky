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

import android.app.NotificationManager
import android.content.Context
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.entity.Notification.Type
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber

object UnifiedPushHelper {

    fun enableUnifiedPushNotificationsForAccount(
        context: Context,
        account: AccountEntity?
    ) {
        Timber.d("Registering UnifiedPush for ${account?.username}")

        account?.let {
            createNotificationsChannels(account, context)

            UnifiedPush.registerAppWithDialog(
                context,
                it.id.toString(),
                features = arrayListOf(UnifiedPush.FEATURE_BYTES_MESSAGE)
            )
        } ?: Timber.e("Account cannot be null")
    }

    fun buildPushDataMap(
        notificationManager: NotificationManager,
        account: AccountEntity?
    ): Map<String, Boolean> {
        return buildMap {
            Type.asList.forEach {type ->
                put(
                    "data[alerts][${type.presentation}]",
                    NotificationHelper.filterNotification(account, type, notificationManager)
                )
            }
        }
    }

    private fun createNotificationsChannels(account: AccountEntity, applicationContext: Context) {
        NotificationHelper.createNotificationChannelsForAccount(account, applicationContext)
    }
}
