/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
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

package com.husky.project.core.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.husky.project.core.di.HuskyServices
import com.keylesspalace.tusky.core.extensions.viewBinding
import com.keylesspalace.tusky.databinding.ActivityNavigationBinding
import com.husky.project.features.splash.view.navigation.SplashKey
import com.zhuinden.simplestack.History
import com.zhuinden.simplestack.SimpleStateChanger
import com.zhuinden.simplestack.StateChange
import com.zhuinden.simplestack.navigator.Navigator
import com.zhuinden.simplestackextensions.fragments.DefaultFragmentStateChanger
import com.zhuinden.simplestackextensions.navigatorktx.backstack
import com.zhuinden.simplestackextensions.services.DefaultServiceProvider
import timber.log.Timber

class NavigationActivity : AppCompatActivity(), SimpleStateChanger.NavigationHandler {

    private val binding by viewBinding(ActivityNavigationBinding::inflate)
    private lateinit var fragmentStateChanger: DefaultFragmentStateChanger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initNavigation()
    }

    override fun onBackPressed() {
        if(!backstack.goBack()) {
            Timber.i("No keys found, exiting the application...")

            this.finishAndRemoveTask()
        }
    }

    override fun onNavigationEvent(stateChange: StateChange) {
        fragmentStateChanger.handleStateChange(stateChange)
    }

    private fun initNavigation() {
        fragmentStateChanger = DefaultFragmentStateChanger(
            supportFragmentManager,
            binding.fragmentContainer.id
        )

        Navigator.configure()
            .setStateChanger(SimpleStateChanger(this))
            .setScopedServices(DefaultServiceProvider())
            .setGlobalServices(HuskyServices(applicationContext).getGlobalServices())
            .install(
                this,
                binding.fragmentContainer,
                History.single(SplashKey())
            )

        Timber.d("Navigation setup completely")
    }
}
