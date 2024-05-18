/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2018  Conny Duck
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

package com.keylesspalace.tusky.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.ChatMessageDeliveredEvent
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.StatusComposedEvent
import com.keylesspalace.tusky.appstore.StatusPreviewEvent
import com.keylesspalace.tusky.appstore.StatusScheduledEvent
import com.keylesspalace.tusky.components.drafts.DraftHelper
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.core.functional.Either
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.entity.ChatMessage
import com.keylesspalace.tusky.entity.NewChatMessage
import com.keylesspalace.tusky.entity.NewPoll
import com.keylesspalace.tusky.entity.NewStatus
import com.keylesspalace.tusky.entity.Status
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.util.SaveTootHelper
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SendTootService : Service(), KoinComponent {

    private val mastodonApi: MastodonApi by inject()
    private val accountManager: AccountManager by inject()
    private val eventHub: EventHub by inject()
    private val draftHelper: DraftHelper by inject()
    private val saveTootHelper: SaveTootHelper by inject()

    private val tootsToSend = ConcurrentHashMap<Int, PostToSend>()
    private val sendCalls = ConcurrentHashMap<Int, Either<Call<Status>, Call<ChatMessage>>>()

    private val timer = Timer()

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.hasExtra(KEY_CANCEL)) {
            cancelSending(intent.getIntExtra(KEY_CANCEL, 0))
            return START_NOT_STICKY
        }

        val postToSend: PostToSend = (
            intent.getParcelableExtra<TootToSend>(KEY_TOOT)
                ?: intent.getParcelableExtra<MessageToSend>(KEY_CHATMSG)
            ) as PostToSend?
            ?: throw IllegalStateException("SendTootService started without $KEY_CHATMSG or $KEY_TOOT extra")

        if (NotificationHelper.NOTIFICATION_USE_CHANNELS) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.send_toot_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(getString(R.string.send_toot_notification_title))
            .setContentText(postToSend.getNotificationText())
            .setProgress(1, 0, true)
            .setOngoing(true)
            .setColor(ContextCompat.getColor(this, R.color.tusky_blue))
            .addAction(
                0,
                getString(android.R.string.cancel),
                cancelSendingIntent(sendingNotificationId)
            )

        if (tootsToSend.size == 0 || VERSION.SDK_INT >= VERSION_CODES.O) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                startForeground(
                    sendingNotificationId,
                    builder.build(),
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    sendingNotificationId,
                    builder.build()
                )
            }
        } else {
            notificationManager.notify(sendingNotificationId, builder.build())
        }

        tootsToSend[sendingNotificationId] = postToSend
        sendToot(sendingNotificationId--)

        return START_NOT_STICKY
    }

    private fun sendToot(tootId: Int) {
        // when tootToSend == null, sending has been canceled
        val postToSend = tootsToSend[tootId] ?: return

        // when account == null, user has logged out, cancel sending
        val account = accountManager.getAccountById(postToSend.getAccountId())

        if (account == null) {
            tootsToSend.remove(tootId)
            notificationManager.cancel(tootId)
            stopSelfWhenDone()
            return
        }

        postToSend.incrementRetries()

        if (postToSend is TootToSend) {
            val contentType: String? = postToSend.formattingSyntax.ifEmpty { null }
            val preview: Boolean? = if (postToSend.preview) true else null

            val newStatus = NewStatus(
                postToSend.text,
                postToSend.warningText,
                postToSend.inReplyToId,
                postToSend.visibility,
                postToSend.sensitive,
                postToSend.mediaIds,
                postToSend.scheduledAt,
                postToSend.expiresIn,
                postToSend.poll,
                contentType,
                preview,
                postToSend.quoteId
            )

            val sendCall = mastodonApi.createStatus(
                "Bearer " + account.accessToken,
                account.domain,
                postToSend.idempotencyKey,
                newStatus
            )

            val callback = object : Callback<Status> {
                override fun onResponse(call: Call<Status>, response: Response<Status>) {
                    val scheduled = !postToSend.scheduledAt.isNullOrEmpty()
                    tootsToSend.remove(tootId)

                    if (response.isSuccessful) {
                        // If the status was loaded from a draft, delete the draft and associated media files.
                        if (postToSend.savedTootUid != 0) {
                            saveTootHelper.deleteDraft(postToSend.savedTootUid)
                        }
                        if (postToSend.draftId != 0) {
                            draftHelper.deleteDraftAndAttachments(postToSend.draftId)
                                .subscribe()
                        }

                        when {
                            postToSend.preview -> response.body()?.let(::StatusPreviewEvent)
                                ?.let(eventHub::dispatch)
                            scheduled -> response.body()?.let(::StatusScheduledEvent)
                                ?.let(eventHub::dispatch)
                            else -> response.body()?.let(::StatusComposedEvent)
                                ?.let(eventHub::dispatch)
                        }
                        notificationManager.cancel(tootId)
                    } else {
                        // the server refused to accept the toot, save toot & show error message
                        saveTootToDrafts(postToSend)

                        val builder = NotificationCompat.Builder(this@SendTootService, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notify)
                            .setContentTitle(getString(R.string.send_toot_notification_error_title))
                            .setContentText(getString(R.string.send_toot_notification_saved_content))
                            .setColor(
                                ContextCompat.getColor(
                                    this@SendTootService,
                                    R.color.tusky_blue
                                )
                            )

                        notificationManager.cancel(tootId)
                        notificationManager.notify(errorNotificationId--, builder.build())
                    }

                    stopSelfWhenDone()
                }

                override fun onFailure(call: Call<Status>, t: Throwable) {
                    var backoff = TimeUnit.SECONDS.toMillis(postToSend.retries.toLong())
                    if (backoff > MAX_RETRY_INTERVAL) {
                        backoff = MAX_RETRY_INTERVAL
                    }

                    timer.schedule(
                        object : TimerTask() {
                            override fun run() {
                                sendToot(tootId)
                            }
                        },
                        backoff
                    )
                }
            }

            sendCalls[tootId] = Either.Left(sendCall)
            sendCall.enqueue(callback)
        } else if (postToSend is MessageToSend) {
            val newMessage = NewChatMessage(postToSend.text, postToSend.mediaId)

            val sendCall = mastodonApi.createChatMessage(
                "Bearer " + account.accessToken,
                account.domain,
                postToSend.chatId,
                newMessage
            )

            val callback = object : Callback<ChatMessage> {
                override fun onResponse(call: Call<ChatMessage>, response: Response<ChatMessage>) {
                    tootsToSend.remove(tootId)

                    if (response.isSuccessful) {
                        notificationManager.cancel(tootId)

                        eventHub.dispatch(ChatMessageDeliveredEvent(response.body()!!))
                    } else {
                        val builder = NotificationCompat.Builder(this@SendTootService, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notify)
                            .setContentTitle(getString(R.string.send_toot_notification_error_title))
                            .setContentText(getString(R.string.send_toot_notification_saved_content))
                            .setColor(
                                ContextCompat.getColor(
                                    this@SendTootService,
                                    R.color.tusky_blue
                                )
                            )

                        notificationManager.cancel(tootId)
                        notificationManager.notify(errorNotificationId--, builder.build())
                    }

                    stopSelfWhenDone()
                }

                override fun onFailure(call: Call<ChatMessage>, t: Throwable) {
                    var backoff = TimeUnit.SECONDS.toMillis(postToSend.retries.toLong())
                    if (backoff > MAX_RETRY_INTERVAL) {
                        backoff = MAX_RETRY_INTERVAL
                    }

                    timer.schedule(
                        object : TimerTask() {
                            override fun run() {
                                sendToot(tootId)
                            }
                        },
                        backoff
                    )
                }
            }

            sendCalls[tootId] = Either.Right(sendCall)
            sendCall.enqueue(callback)
        }
    }

    private fun stopSelfWhenDone() {
        if (tootsToSend.isEmpty()) {
            ServiceCompat.stopForeground(this@SendTootService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun cancelSending(tootId: Int) {
        val tootToCancel = tootsToSend.remove(tootId)
        if (tootToCancel != null) {
            val sendCall = sendCalls.remove(tootId)

            sendCall?.let {
                if (it.isLeft()) {
                    val sendStatusCall = it.asLeft()
                    sendStatusCall.cancel()

                    saveTootToDrafts(tootToCancel as TootToSend)
                } else {
                    val sendMessageCall = it.asRight()
                    sendMessageCall.cancel()
                }
            }

            val builder = NotificationCompat.Builder(this@SendTootService, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(getString(R.string.send_toot_notification_cancel_title))
                .setContentText(getString(R.string.send_toot_notification_saved_content))
                .setColor(ContextCompat.getColor(this@SendTootService, R.color.tusky_blue))

            notificationManager.notify(tootId, builder.build())

            timer.schedule(
                object : TimerTask() {
                    override fun run() {
                        notificationManager.cancel(tootId)
                        stopSelfWhenDone()
                    }
                },
                5000
            )
        }
    }

    private fun saveTootToDrafts(toot: TootToSend) {
        draftHelper.saveDraft(
            draftId = toot.draftId,
            accountId = toot.getAccountId(),
            inReplyToId = toot.inReplyToId,
            content = toot.text,
            contentWarning = toot.warningText,
            sensitive = toot.sensitive,
            visibility = Status.Visibility.byString(toot.visibility),
            mediaUris = toot.mediaUris,
            mediaDescriptions = toot.mediaDescriptions,
            poll = toot.poll,
            formattingSyntax = toot.formattingSyntax,
            failedToSend = true,
            quoteId = toot.quoteId
        ).subscribe()
    }

    private fun cancelSendingIntent(tootId: Int): PendingIntent {
        val intent = Intent(this, SendTootService::class.java)
        intent.putExtra(KEY_CANCEL, tootId)
        val flags =
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        return PendingIntent.getService(
            this,
            tootId,
            intent,
            flags
        )
    }

    companion object {

        private const val KEY_CHATMSG = "chatmsg"
        private const val KEY_TOOT = "toot"
        private const val KEY_CANCEL = "cancel_id"
        private const val CHANNEL_ID = "send_toots"

        private val MAX_RETRY_INTERVAL = TimeUnit.MINUTES.toMillis(1)

        private var sendingNotificationId = -1 // use negative ids to not clash with other notis
        private var errorNotificationId =
            Int.MIN_VALUE // use even more negative ids to not clash with other notis

        private fun Intent.forwardUriPermissions(mediaUris: List<String>) {
            if (mediaUris.isEmpty()) {
                return
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uriClip = ClipData(
                ClipDescription("Toot Media", arrayOf("image/*", "video/*")),
                ClipData.Item(mediaUris[0])
            )
            mediaUris.drop(1).forEach { uriClip.addItem(ClipData.Item(it)) }

            clipData = uriClip
        }

        @JvmStatic
        fun sendMessageIntent(context: Context, msgToSend: MessageToSend): Intent {
            val intent = Intent(context, SendTootService::class.java)
            intent.putExtra(KEY_CHATMSG, msgToSend)
            if (msgToSend.mediaUri != null) {
                intent.forwardUriPermissions(listOf(msgToSend.mediaUri))
            }

            return intent
        }

        @JvmStatic
        fun sendTootIntent(context: Context, tootToSend: TootToSend): Intent {
            val intent = Intent(context, SendTootService::class.java)
            intent.putExtra(KEY_TOOT, tootToSend)
            intent.forwardUriPermissions(tootToSend.mediaUris)

            return intent
        }
    }
}

interface PostToSend {
    fun getAccountId(): Long
    fun getNotificationText(): String
    fun incrementRetries()
}

@Parcelize
data class MessageToSend(
    val text: String,
    val mediaId: String?,
    val mediaUri: String?,
    private val accountId: Long,
    val chatId: String,
    var retries: Int
) : Parcelable, PostToSend {
    override fun getAccountId(): Long {
        return accountId
    }

    override fun getNotificationText(): String {
        return text
    }

    override fun incrementRetries() {
        retries++
    }
}

@Parcelize
data class TootToSend(
    val text: String,
    val warningText: String,
    val visibility: String,
    val sensitive: Boolean,
    val mediaIds: List<String>,
    val mediaUris: List<String>,
    val mediaDescriptions: List<String>,
    val scheduledAt: String?,
    val expiresIn: Int?,
    val inReplyToId: String?,
    val poll: NewPoll?,
    val replyingStatusContent: String?,
    val replyingStatusAuthorUsername: String?,
    val formattingSyntax: String,
    val preview: Boolean,
    private val accountId: Long,
    val savedTootUid: Int,
    val draftId: Int,
    val idempotencyKey: String,
    var retries: Int,
    val quoteId: String?
) : Parcelable, PostToSend {

    override fun getNotificationText(): String {
        return warningText.ifBlank {
            text
        }
    }

    override fun getAccountId(): Long {
        return accountId
    }

    override fun incrementRetries() {
        retries++
    }
}
