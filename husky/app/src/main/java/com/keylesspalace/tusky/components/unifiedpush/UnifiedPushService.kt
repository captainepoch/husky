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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.crypto.CryptoConstants
import com.keylesspalace.tusky.core.crypto.CryptoECKeyPair
import com.keylesspalace.tusky.core.crypto.CryptoUtils
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.network.MastodonApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.security.Security

class UnifiedPushService : Service(), KoinComponent {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var notificationManager: NotificationManager
    private val api by inject<MastodonApi>()
    private val accountManager by inject<AccountManager>()
    private var serviceStarted = false

    override fun onCreate() {
        super.onCreate()

        Timber.d("Creating service")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("Destroy service")

        serviceStarted = false
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("Binding the service")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!serviceStarted) {
            Timber.d("Service not created")
            createServiceNotification()
            Timber.d("Create push notification")

            modifySecurityProvider(true)
            modifySecurityProvider()

            val endpoint = intent?.getStringExtra(ENDPOINT)
            if (endpoint.isNullOrEmpty() || endpoint.isBlank()) {
                stopSelf()
            } else {
                Timber.d("Subscribing to push notifications")
                subscribeToPush(generateKeyPair(), getAuth(), endpoint)
                Timber.d("Subscribed to push notifications")
            }

            serviceStarted = true
        }

        return START_STICKY
    }

    private fun createServiceNotification() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.unifiedpush_service_foreground_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.unifiedpush_service_foreground_title))
            .setContentText(getString(R.string.unifiedpush_service_foreground_text))
            .setSmallIcon(R.drawable.ic_husky)
            .build()

        startForeground(PUSH_SUBS_ID, notification)
    }

    private fun modifySecurityProvider(delete: Boolean = false) {
        if (delete) {
            Timber.d("Deleting the security provider")

            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        } else {
            Timber.d("Adding the security provider")

            Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun generateKeyPair(): CryptoECKeyPair {
        Timber.d("Generating a key pair")
        return CryptoUtils.generateECPair(CryptoConstants.CURVE_PRIME256_V1)
    }

    private fun getAuth(): String {
        Timber.d("Generating a random 16 byte string")
        return CryptoUtils.getSecureRandomStringBase64(16)
    }

    private fun subscribeToPush(keyPair: CryptoECKeyPair, auth: String, endpoint: String) {
        scope.launch {
            Timber.d("Subscribing to the push notification service")

            accountManager.activeAccount?.let { account ->
                val response = api.subscribePushNotifications(
                    "Bearer ${account.accessToken}",
                    account.domain,
                    endpoint,
                    keyPair.pubKey,
                    auth,
                    UnifiedPushHelper.buildPushDataMap(notificationManager, account)
                )

                if (response.body() != null) {
                    Timber.d("UnifiedPush registration for ${account.username}")

                    account.unifiedPushUrl = endpoint
                    accountManager.saveAccount(account)

                    // TODO: Update push notification

                    stopSelf()
                    return@launch
                }

                if (response.errorBody() != null) {
                    Timber.e("Error in the response [${response.raw().message}]")

                    // TODO: See what to do with an error

                    stopSelf()
                    return@launch
                }

                null
            } ?: runCatching {
                stopSelf()
                return@launch
            }
        }
    }

    companion object {

        private const val PUSH_SUBS_ID = 200
        private const val CHANNEL_ID = "2000"
        private const val ENDPOINT = "ENDPOINT"

        fun startService(context: Context, endpoint: String) {
            val intent = Intent(context, UnifiedPushService::class.java)
            intent.putExtra(ENDPOINT, endpoint)

            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, UnifiedPushService::class.java)
            context.stopService(intent)
        }
    }
}
