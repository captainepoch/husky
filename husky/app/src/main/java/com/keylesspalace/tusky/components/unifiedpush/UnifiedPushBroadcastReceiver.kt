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
import org.unifiedpush.android.connector.MessagingReceiver
import timber.log.Timber

class UnifiedPushBroadcastReceiver : MessagingReceiver() {

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        super.onMessage(context, message, instance)
        Timber.d("New message for $instance")
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        super.onNewEndpoint(context, endpoint, instance)
        Timber.d("New endpoint for instance $instance")

        UnifiedPushService.startService(context, endpoint)
    }

    override fun onRegistrationFailed(context: Context, instance: String) {
        super.onRegistrationFailed(context, instance)
        Timber.d("Registration failed for $instance")
    }

    override fun onUnregistered(context: Context, instance: String) {
        super.onUnregistered(context, instance)
        Timber.d("Unregister for $instance")
    }
}
