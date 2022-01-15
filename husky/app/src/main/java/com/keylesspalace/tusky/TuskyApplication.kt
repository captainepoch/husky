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
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.core.app.NotificationManagerCompat
import androidx.emoji.text.EmojiCompat
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideCustomImageLoader
import com.keylesspalace.tusky.components.notifications.NotificationWorkerFactory
import com.keylesspalace.tusky.core.logging.HyperlinkDebugTree
import com.keylesspalace.tusky.core.utils.ApplicationUtils
import com.keylesspalace.tusky.di.AppInjector
import com.keylesspalace.tusky.settings.PrefKeys
import com.keylesspalace.tusky.util.EmojiCompatFont
import com.keylesspalace.tusky.util.LocaleManager
import com.keylesspalace.tusky.util.ThemeUtils
import com.uber.autodispose.AutoDisposePlugins
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import java.security.Security
import javax.inject.Inject
import org.acra.ReportField.ANDROID_VERSION
import org.acra.ReportField.APP_VERSION_CODE
import org.acra.ReportField.APP_VERSION_NAME
import org.acra.ReportField.BUILD_CONFIG
import org.acra.ReportField.STACK_TRACE
import org.acra.config.mailSender
import org.acra.config.notification
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.conscrypt.Conscrypt
import timber.log.Timber

class TuskyApplication : Application(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var notificationWorkerFactory: NotificationWorkerFactory

    override fun onCreate() {
        super.onCreate()

        if(ApplicationUtils.isDebug()) {
            Timber.plant(HyperlinkDebugTree())
        }

        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        AutoDisposePlugins.setHideProxies(false) // a small performance optimization

        AppInjector.init(this)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // init the custom emoji fonts
        val emojiSelection = preferences.getInt(PrefKeys.EMOJI, 0)
        val emojiConfig = EmojiCompatFont.byId(emojiSelection)
            .getConfig(this)
            .setReplaceAll(true)
        EmojiCompat.init(emojiConfig)

        // init night mode
        val theme = preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT)
        ThemeUtils.setAppNightMode(theme)

        RxJavaPlugins.setErrorHandler {
            Timber.tag("RxJava").w("undeliverable exception: $it")
        }

        SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
        BigImageViewer.initialize(GlideCustomImageLoader.with(this))

        WorkManager.initialize(
            this,
            androidx.work.Configuration.Builder()
                .setWorkerFactory(notificationWorkerFactory)
                .build()
        )
    }

    override fun attachBaseContext(base: Context) {
        localeManager = LocaleManager(base)
        super.attachBaseContext(localeManager.setLocale(base))

        setupAcra()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.setLocale(this)
    }

    override fun androidInjector() = androidInjector

    companion object {
        @JvmStatic
        lateinit var localeManager: LocaleManager
    }

    private fun setupAcra() {
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST
            reportContent = listOf(
                ANDROID_VERSION,
                APP_VERSION_NAME,
                APP_VERSION_CODE,
                BUILD_CONFIG,
                STACK_TRACE
            ).toTypedArray()

            notification {
                title = getString(R.string.acra_notification_title)
                text = getString(R.string.acra_notification_body)
                channelName = getString(R.string.acra_notification_channel_title)
                channelDescription = getString(R.string.acra_notification_channel_body)
                resChannelImportance = NotificationManagerCompat.IMPORTANCE_DEFAULT
                //resIcon = R.drawable.notification_icon
                sendButtonText = getString(R.string.acra_notification_report)
                //resSendButtonIcon = R.drawable.notification_send
                discardButtonText = getString(R.string.acra_notification_discard)
                //resDiscardButtonIcon = R.drawable.notification_discard
                sendOnClick = false
            }

            mailSender {
                mailTo = getString(R.string.acra_email)
                reportAsFile = false
                reportFileName = getString(R.string.acra_email_report_filename)
                subject = getString(R.string.acra_email_subject)
            }
        }
    }
}
