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
import com.keylesspalace.tusky.entity.Notification.Type.FAVOURITE
import com.keylesspalace.tusky.entity.Notification.Type.FOLLOW
import com.keylesspalace.tusky.entity.Notification.Type.FOLLOW_REQUEST
import com.keylesspalace.tusky.entity.Notification.Type.MENTION
import com.keylesspalace.tusky.entity.Notification.Type.POLL
import com.keylesspalace.tusky.entity.Notification.Type.REBLOG
import com.keylesspalace.tusky.entity.Notification.Type.STATUS
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
            Type.asList.forEach {
                put(
                    "data[alerts][${it.presentation}]",
                    filterNotificationType(notificationManager, account, it)
                )
            }
        }
    }

    private fun createNotificationsChannels(account: AccountEntity, applicationContext: Context) {
        NotificationHelper.createNotificationChannelsForAccount(account, applicationContext)
    }

    private fun filterNotificationType(
        notificationManager: NotificationManager,
        account: AccountEntity?,
        type: Type
    ): Boolean {
        if (NotificationHelper.NOTIFICATION_USE_CHANNELS) {
            val channelId = getChannelId(account, type) ?: return false

            val channel = notificationManager.getNotificationChannel(channelId)
            return (channel != null && channel.importance > NotificationManager.IMPORTANCE_NONE)
        }

        return when (type) {
            MENTION -> account?.notificationsMentioned ?: false
            STATUS -> account?.notificationsSubscriptions ?: false
            FOLLOW -> account?.notificationsFollowed ?: false
            FOLLOW_REQUEST -> account?.notificationsFollowRequested ?: false
            REBLOG -> account?.notificationsReblogged ?: false
            FAVOURITE -> account?.notificationsFavorited ?: false
            POLL -> account?.notificationsPolls ?: false
            else -> false
        }
    }

    private fun getChannelId(account: AccountEntity?, type: Type): String? {
        return when (type) {
            MENTION -> NotificationHelper.CHANNEL_MENTION + account?.identifier
            STATUS -> NotificationHelper.CHANNEL_SUBSCRIPTIONS + account?.identifier
            FOLLOW -> NotificationHelper.CHANNEL_FOLLOW + account?.identifier
            FOLLOW_REQUEST -> NotificationHelper.CHANNEL_FOLLOW_REQUEST + account?.identifier
            REBLOG -> NotificationHelper.CHANNEL_BOOST + account?.identifier
            FAVOURITE -> NotificationHelper.CHANNEL_FAVOURITE + account?.identifier
            POLL -> NotificationHelper.CHANNEL_POLL + account?.identifier
            else -> null
        }
    }
}
