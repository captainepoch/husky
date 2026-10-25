package com.keylesspalace.tusky.core

import com.keylesspalace.tusky.core.utils.Flavor

interface HuskyAppConfig {
    val applicationId: String
    val applicationName: String
    val versionName: String
    val versionCode: Long
    val isDebug: Boolean
    val flavor: Flavor
    val customInstance: String
    val customLogoUrl: String
    val supportAccountUrl: String
    val userAgent: String

    val fileProviderAuthority: String get() = "$applicationId.fileprovider"
}
