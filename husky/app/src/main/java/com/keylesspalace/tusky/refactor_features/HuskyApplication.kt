package com.keylesspalace.tusky.refactor_features

import android.app.Application
import com.keylesspalace.tusky.core.logging.HyperlinkDebugTree
import com.keylesspalace.tusky.core.utils.ApplicationUtils
import timber.log.Timber

class HuskyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        enableDebugConfig()
    }

    /**
     * Enable debug settings at startup.
     */
    private fun enableDebugConfig() {
        if(ApplicationUtils.isDebug()) {
            Timber.plant(HyperlinkDebugTree())

            Timber.d("Debug config enabled")
        }
    }
}
