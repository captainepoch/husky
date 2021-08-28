/* Copyright 2020 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.emoji.text.EmojiCompat
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.util.LocaleManager
import com.keylesspalace.tusky.util.OmittedDomainAppModule
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import de.c1710.filemojicompat.FileEmojiCompatConfig
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import javax.inject.Inject

// override TuskyApplication for Robolectric tests, only initialize the necessary stuff
class TuskyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        EmojiCompat.init(FileEmojiCompatConfig(this, ""))
    }

    override fun attachBaseContext(base: Context) {
        localeManager = LocaleManager(base)
        super.attachBaseContext(localeManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.setLocale(this)
    }

    fun getAndroidInjector() : DispatchingAndroidInjector<Any> {
        val mock = mock(DispatchingAndroidInjector::class.java) as DispatchingAndroidInjector<OmittedDomainAppModule>

        `when`(mock.inject(any())).then {
            it.getArgument<OmittedDomainAppModule>(0).accountManager = mock(AccountManager::class.java)
            return@then Unit
        }
        return mock as DispatchingAndroidInjector<Any>
    }

    companion object {
        @JvmStatic
        lateinit var localeManager: LocaleManager
    }
}
