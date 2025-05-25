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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.keylesspalace.tusky.components.notifications.NotificationHelper
import com.keylesspalace.tusky.viewmodel.SplashViewModel
import com.keylesspalace.tusky.viewmodel.viewstate.SplashViewState
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SplashActivity : AppCompatActivity() {

    private val viewModel by viewModel<SplashViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Delete old notification channels
        NotificationHelper.deleteLegacyNotificationChannels(this, viewModel.accountManager)

        collectStates()
    }

    private fun collectStates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.splashViewState.collect { state ->
                    val intent = when (state) {
                        SplashViewState.USER -> {
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }

                        SplashViewState.NO_USER -> {
                            LoginActivity.getIntent(this@SplashActivity, false)
                        }

                        else -> null
                    }

                    intent?.let {
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
}
