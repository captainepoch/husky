/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2020  Alibek Omarov
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
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.appstore.ChatMessageReceivedEvent
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountEntity
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.di.Injectable
import com.keylesspalace.tusky.entity.Notification
import com.keylesspalace.tusky.entity.StreamEvent
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.isLessThan
import dagger.android.AndroidInjection
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

class StreamingService : Service(), Injectable {

    @Inject
    lateinit var api: MastodonApi

    @Inject
    lateinit var eventHub: EventHub

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var client: OkHttpClient

    private val sockets: MutableMap<Long, WebSocket> = mutableMapOf()

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    private fun stopStreamingForId(id: Long) {
        if(id in sockets) {
            sockets[id]?.close(1000, null)
            sockets.remove(id)
        }
    }

    private fun stopStreaming() {
        for(sock in sockets) {
            sock.value.close(1000, null)
        }
        sockets.clear()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }

        notificationManager.cancel(1337)

        synchronized(serviceRunning) {
            serviceRunning = false
        }
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if(intent.getBooleanExtra(KEY_STOP_STREAMING, false)) {
            Timber.d("Stream goes suya..")
            stopStreaming()
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        var description = getString(R.string.streaming_notification_description)
        val accounts = accountManager.getAllAccountsOrderedByActive()
        var count = 0
        for(account in accounts) {
            stopStreamingForId(account.id)

            if(!account.notificationsStreamingEnabled) {
                continue
            }

            val endpoint =
                "wss://${account.domain}/api/v1/streaming/?access_token=${account.accessToken}&stream=user:notification"
            val request = Request.Builder().url(endpoint).build()

            Timber.d("Running stream for ${account.fullName}")

            sockets[account.id] = client.newWebSocket(
                request,
                makeStreamingListener(
                    "${account.fullName}/user:notification",
                    account
                )
            )

            description += "\n" + account.fullName
            count++
        }

        if(count <= 0) {
            Timber.d("No accounts. Stopping stream")
            stopStreaming()
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        if(NotificationHelper.NOTIFICATION_USE_CHANNELS) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.streaming_notification_name),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(getString(R.string.streaming_notification_name))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setColor(ContextCompat.getColor(this, R.color.tusky_blue))

        val showDescription = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(PrefKeys.HIDE_LIVE_NOTIFICATION_DESCRIPTION, false)
        if(!showDescription) {
            builder.setContentText(description)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            startForeground(1337, builder.build())
        } else {
            notificationManager.notify(1337, builder.build())
        }

        synchronized(serviceRunning) {
            serviceRunning = true
        }

        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "streaming"
        const val KEY_STOP_STREAMING = "stop_streaming"

        @JvmStatic
        var serviceRunning = false

        @JvmStatic
        private fun startForegroundService(ctx: Context, intent: Intent) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        @JvmStatic
        fun startStreaming(context: Context) {
            val intent = Intent(context, StreamingService::class.java)
            intent.putExtra(KEY_STOP_STREAMING, false)

            Timber.d("Starting notifications streaming service...")

            startForegroundService(context, intent)
        }

        @JvmStatic
        fun stopStreaming(context: Context) {
            synchronized(serviceRunning) {
                if(!serviceRunning) {
                    return
                }

                val intent = Intent(context, StreamingService::class.java)
                intent.putExtra(KEY_STOP_STREAMING, true)

                Timber.d("Stopping notifications streaming service...")

                serviceRunning = false

                startForegroundService(context, intent)
            }
        }
    }

    private fun makeStreamingListener(tag: String, account: AccountEntity): WebSocketListener {

        return object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("Stream connected to: $tag. Response[$response]")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("Stream closed for: $tag. Reason[$reason]")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e("Stream failed for $tag: $t. Response[$response]")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = gson.fromJson(text, StreamEvent::class.java)
                when(event.event) {
                    StreamEvent.EventType.NOTIFICATION -> {
                        val notification = gson.fromJson(event.payload, Notification::class.java)
                        NotificationHelper.make(this@StreamingService, notification, account, true)

                        if(notification.type == Notification.Type.CHAT_MESSAGE) {
                            eventHub.dispatch(ChatMessageReceivedEvent(notification.chatMessage!!))
                        }

                        if(account.lastNotificationId.isLessThan(notification.id)) {
                            account.lastNotificationId = notification.id
                            accountManager.saveAccount(account)
                        }
                    }
                    else -> {
                        Timber.w("Unknown event type: ${event.event}")
                    }
                }
            }
        }
    }
}
