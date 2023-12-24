/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Jeremiasz Nelz
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

package com.keylesspalace.tusky.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.components.compose.ComposeActivity
import com.keylesspalace.tusky.components.compose.ComposeActivity.ComposeOptions
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.service.SendTootService
import com.keylesspalace.tusky.service.TootToSend
import com.keylesspalace.tusky.util.randomAlphanumericString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class SendStatusBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val accountManager: AccountManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.KEY_NOTIFICATION_ID, -1)
        val senderId = intent.getLongExtra(NotificationHelper.KEY_SENDER_ACCOUNT_ID, -1)
        val senderIdentifier =
            intent.getStringExtra(NotificationHelper.KEY_SENDER_ACCOUNT_IDENTIFIER)
        val senderFullName = intent.getStringExtra(NotificationHelper.KEY_SENDER_ACCOUNT_FULL_NAME)
        val citedStatusId = intent.getStringExtra(NotificationHelper.KEY_CITED_STATUS_ID)
        val visibility =
            intent.getSerializableExtra(NotificationHelper.KEY_VISIBILITY) as Status.Visibility
        val spoiler = intent.getStringExtra(NotificationHelper.KEY_SPOILER) ?: ""
        val mentions = intent.getStringArrayExtra(NotificationHelper.KEY_MENTIONS) ?: emptyArray()
        val citedText = intent.getStringExtra(NotificationHelper.KEY_CITED_TEXT)
        val localAuthorId = intent.getStringExtra(NotificationHelper.KEY_CITED_AUTHOR_LOCAL)

        val account = accountManager.getAccountById(senderId)

        val notificationManager = NotificationManagerCompat.from(context)

        if (intent.action == NotificationHelper.REPLY_ACTION) {
            val message = getReplyMessage(intent)

            if (account == null) {
                Timber.w("Account [$senderId] not found in database. Aborting quick reply!")

                val builder = NotificationCompat.Builder(
                    context,
                    NotificationHelper.CHANNEL_MENTION + senderIdentifier
                )
                    .setSmallIcon(R.drawable.ic_notify)
                    .setColor(ContextCompat.getColor(context, R.color.tusky_blue))
                    .setGroup(senderFullName)
                    .setDefaults(0) // So it doesn't ring twice, notify only in Target callback

                builder.setContentTitle(context.getString(R.string.error_generic))
                builder.setContentText(context.getString(R.string.error_sender_account_gone))

                builder.setSubText(senderFullName)
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
                builder.setOnlyAlertOnce(true)

                notificationManager.notify(notificationId, builder.build())
            } else {
                val text = mentions.joinToString(" ", postfix = " ") { "@$it" } + message.toString()

                val sendIntent = SendTootService.sendTootIntent(
                    context,
                    TootToSend(
                        text = text,
                        warningText = spoiler,
                        visibility = visibility.serverString(),
                        sensitive = false,
                        mediaIds = emptyList(),
                        mediaUris = emptyList(),
                        mediaDescriptions = emptyList(),
                        scheduledAt = null,
                        expiresIn = accountManager.activeAccount?.postExpiresIn,
                        inReplyToId = citedStatusId,
                        poll = null,
                        replyingStatusContent = null,
                        replyingStatusAuthorUsername = null,
                        accountId = account.id,
                        draftId = -1,
                        idempotencyKey = randomAlphanumericString(16),
                        retries = 0,
                        formattingSyntax = "",
                        preview = false,
                        savedTootUid = -1,
                        quoteId = null
                    )
                )

                context.startService(sendIntent)

                val builder = NotificationCompat.Builder(
                    context,
                    NotificationHelper.CHANNEL_MENTION + senderIdentifier
                )
                    .setSmallIcon(R.drawable.ic_notify)
                    .setColor(ContextCompat.getColor(context, (R.color.tusky_blue)))
                    .setGroup(senderFullName)
                    .setDefaults(0) // So it doesn't ring twice, notify only in Target callback

                builder.setContentTitle(context.getString(R.string.status_sent))
                builder.setContentText(context.getString(R.string.status_sent_long))

                builder.setSubText(senderFullName)
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
                builder.setOnlyAlertOnce(true)

                notificationManager.notify(notificationId, builder.build())
            }
        } else if (intent.action == NotificationHelper.COMPOSE_ACTION) {
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

            notificationManager.cancel(notificationId)

            accountManager.setActiveAccount(senderId)

            val composeIntent = ComposeActivity.startIntent(
                context,
                ComposeOptions(
                    inReplyToId = citedStatusId,
                    replyVisibility = visibility,
                    contentWarning = spoiler,
                    mentionedUsernames = mentions.toSet(),
                    replyingStatusAuthor = localAuthorId,
                    replyingStatusContent = citedText
                )
            )

            composeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(composeIntent)
        }
    }

    private fun getReplyMessage(intent: Intent): CharSequence {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        return remoteInput?.getCharSequence(NotificationHelper.KEY_REPLY, "") ?: ""
    }
}
