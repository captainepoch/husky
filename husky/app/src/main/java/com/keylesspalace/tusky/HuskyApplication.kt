/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2020  Tusky Contributors
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

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.emoji2.text.EmojiCompat
import androidx.work.WorkManager
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideCustomImageLoader
import com.keylesspalace.tusky.core.logging.CrashHandler
import com.keylesspalace.tusky.core.logging.HyperlinkDebugTree
import com.keylesspalace.tusky.core.utils.ApplicationUtils
import com.keylesspalace.tusky.di.appComponentModule
import com.keylesspalace.tusky.di.appModule
import com.keylesspalace.tusky.di.networkModule
import com.keylesspalace.tusky.di.notificationsModule
import com.keylesspalace.tusky.di.repositoryModule
import com.keylesspalace.tusky.di.serviceModule
import com.keylesspalace.tusky.di.useCaseModule
import com.keylesspalace.tusky.di.viewModelsModule
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.EmojiCompatFont
import com.keylesspalace.tusky.util.LocaleManager
import com.keylesspalace.tusky.util.ThemeUtils
import com.uber.autodispose.AutoDisposePlugins
import de.c1710.filemojicompat_defaults.DefaultEmojiPackList
import de.c1710.filemojicompat_ui.helpers.EmojiPackHelper
import io.reactivex.plugins.RxJavaPlugins
import org.conscrypt.Conscrypt
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import java.security.Security

class HuskyApplication : Application() {

    private val crashHandler: CrashHandler by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin()

        val preferences: SharedPreferences = get()
        if (preferences.getBoolean(PrefKeys.CRASH_HANDLER_ENABLE, false)) {
            crashHandler.setAsDefaultHandler()
        }

        setLocaleManager()

        if (ApplicationUtils.isDebug()) {
            Timber.plant(HyperlinkDebugTree())
        }

        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        // Small performance optimization
        AutoDisposePlugins.setHideProxies(false)

        // Init custom emoji fonts
        val emojiSelection = preferences.getInt(PrefKeys.EMOJI, 0)
        val emojiConfig = EmojiCompatFont.byId(emojiSelection)
            .getConfig(this)
            .setReplaceAll(true)
        EmojiCompat.init(emojiConfig)
        EmojiPackHelper.init(this, DefaultEmojiPackList.get(this))

        // Setup default theme
        val theme = preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT)
        ThemeUtils.setAppNightMode(theme)

        RxJavaPlugins.setErrorHandler {
            Timber.tag("RxJava").w("Undeliverable exception: $it")
        }

        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
        BigImageViewer.initialize(GlideCustomImageLoader.with(this))

        WorkManager.initialize(
            this,
            androidx.work.Configuration.Builder()
                .setWorkerFactory(get())
                .build()
        )
    }

    private fun initKoin() {
        startKoin {
            androidLogger()
            androidContext(this@HuskyApplication)
            modules(
                listOf(
                    appComponentModule,
                    appModule,
                    networkModule,
                    notificationsModule,
                    repositoryModule,
                    serviceModule,
                    useCaseModule,
                    viewModelsModule
                )
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.setLocale(this)
    }

    private fun setLocaleManager() {
        localeManager = get<LocaleManager>().apply {
            setLocale(this@HuskyApplication)
        }
    }

    companion object {

        @JvmStatic
        lateinit var localeManager: LocaleManager
    }
}
