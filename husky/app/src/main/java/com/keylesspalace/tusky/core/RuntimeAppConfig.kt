package com.keylesspalace.tusky.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.utils.Flavor

class RuntimeAppConfig(context: Context) : HuskyAppConfig {

    private val packageInfo: PackageInfo = context.packageManager.run {
        if (Build.VERSION.SDK_INT >= 33) {
            getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") getPackageInfo(context.packageName, 0)
        }
    }

    override val applicationId: String = context.packageName
    override val applicationName: String = context.getString(R.string.config_app_name)
    override val versionName: String = packageInfo.versionName.orEmpty()
    override val versionCode: Long = PackageInfoCompat.getLongVersionCode(packageInfo)
    override val isDebug: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    override val flavor: Flavor = Flavor.getFlavor(context.getString(R.string.config_flavor))
    override val customInstance: String = context.getString(R.string.config_custom_instance)
    override val customLogoUrl: String = context.getString(R.string.config_custom_logo_url)
    override val supportAccountUrl: String = context.getString(R.string.config_support_account_url)

    override val userAgent: String =
        "$applicationName/$versionName Android/${Build.VERSION.RELEASE}"
}
