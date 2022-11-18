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

package com.keylesspalace.tusky.core.utils

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.StrictMode
import timber.log.Timber

object DevUtils {

    /**
     * Enable StrictMode on the application.
     */
    fun enableStrictMode() {
        if (ApplicationUtils.isDebug()) {
            val threadPolicyBuilder =
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()

            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                threadPolicyBuilder.detectUnbufferedIo()
            }

            StrictMode.setThreadPolicy(threadPolicyBuilder.build())
            Timber.tag("StrictMode").i("setThreadPolicy")

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            Timber.tag("StrictMode").i("setVmPolicy")
        }
    }
}
