/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
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

package com.keylesspalace.tusky

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.db.AccountManager
import org.koin.android.ext.android.inject

class SplashActivity : AppCompatActivity() {

    private val accountManager: AccountManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        /** delete old notification channels */
        NotificationHelper.deleteLegacyNotificationChannels(this, accountManager)

        /** Determine whether the user is currently logged in, and if so go ahead and load the
         *  timeline. Otherwise, start the activity_login screen. */

        val intent = if (accountManager.activeAccount != null) {
            Intent(this, MainActivity::class.java)
        } else {
            LoginActivity.getIntent(this, false)
        }
        startActivity(intent)
        finish()
    }
}
