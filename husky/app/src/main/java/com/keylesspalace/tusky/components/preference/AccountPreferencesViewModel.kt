package com.keylesspalace.tusky.components.preference

import android.content.Context
import androidx.lifecycle.ViewModel
import com.keylesspalace.tusky.components.unifiedpush.UnifiedPushHelper

class AccountPreferencesViewModel : ViewModel() {

    var hasUnifiedPushProviders = false

    fun init(context: Context) {
        hasUnifiedPushProviders = UnifiedPushHelper.hasUnifiedPushProviders(context)
    }

    fun verifyHasUnifiedPushProviders(context: Context): Boolean {
        return (hasUnifiedPushProviders != UnifiedPushHelper.hasUnifiedPushProviders(context))
    }
}
