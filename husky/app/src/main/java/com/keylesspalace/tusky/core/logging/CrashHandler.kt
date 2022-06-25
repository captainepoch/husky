/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
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

package com.keylesspalace.tusky.core.logging

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Process
import android.os.Process.killProcess
import androidx.core.content.FileProvider
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.core.ui.callbacks.ActivityCallback
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Thread.UncaughtExceptionHandler
import javax.inject.Inject
import kotlin.system.exitProcess
import timber.log.Timber

class CrashHandler @Inject constructor(
    private val huskyApp: Application
) : UncaughtExceptionHandler {

    private val activityCallbacks = object : ActivityCallback() {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            lastActivity = activity
            Timber.d("onActivityCreated[${activity::class.simpleName}]")
        }

        override fun onActivityResumed(activity: Activity) {
            lastActivity = activity
            Timber.d("onActivityResumed[${activity::class.simpleName}]")
        }

        override fun onActivityStopped(activity: Activity) {
            lastActivity = null
            Timber.d("onActivityStopped[${activity::class.simpleName}]")
        }
    }

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private var lastActivity: Activity? = null

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            sendLogEmail(throwable.stackTraceToString())
        } catch(e: IOException) {
            Timber.e("CrashHandler Exception[${e.message}]")
        } finally {
            lastActivity?.let { activity ->
                killApp {
                    activity.finish()
                }
            } ?: killApp {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun getDeviceInfo(): String {
        return StringBuilder().apply {
            this.appendLine("Version name: ${BuildConfig.VERSION_NAME}")
                .appendLine("Version code: ${BuildConfig.VERSION_CODE}")
                .appendLine("OS Version: ${VERSION.RELEASE}")
                .appendLine("SDK: ${VERSION.SDK_INT}")
        }.toString()
    }

    private fun sendLogEmail(stacktrace: String) {
        createCrashesFolder()

        lastActivity?.let { activity ->
            val formattedLog = StringBuilder().apply {
                this.appendLine("## Steps to reproduce the crash:")
                    .appendLine("1. ###")
                    .appendLine("2. ###")
                    .appendLine("...")
                    .appendLine()
                    .appendLine("## Instance: INSTANCE_DOMAIN")
                    .appendLine()
                    .appendLine("## Device details")
                    .appendLine(getDeviceInfo())
                //.appendLine("## Crash details")
                //.appendLine(stacktrace)
            }.toString()
            Timber.d(formattedLog)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(activity.getString(R.string.crashhandler_email))
                )
                putExtra(Intent.EXTRA_SUBJECT, "Husky ${BuildConfig.VERSION_NAME} crash")
                putExtra(Intent.EXTRA_TEXT, formattedLog)
                putExtra(Intent.EXTRA_STREAM, getCrashFileUri(activity, stacktrace))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            if(intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            }
        }
    }

    private fun createCrashesFolder() {
        File("${huskyApp.cacheDir}/crashes").mkdirs()
    }

    private fun getCrashFileUri(activity: Activity, stacktrace: String): Uri {
        val file = File(
            "${huskyApp.cacheDir}/crashes",
            activity.getString(R.string.crashhandler_email_report_filename)
        )
        FileOutputStream(file).apply {
            write(stacktrace.toByteArray())
        }.also {
            it.close()
        }
        return FileProvider.getUriForFile(
            activity,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
    }

    private fun killApp(listener: () -> Unit = {}) {
        listener()

        killProcess(Process.myPid())
        exitProcess(10)
    }

    fun setAsDefaultHandler() {
        val handler = defaultHandler?.let {
            this@CrashHandler
        }
        Thread.setDefaultUncaughtExceptionHandler(handler)
        huskyApp.registerActivityLifecycleCallbacks(activityCallbacks)

        Timber.d("Set default handler[${handler}]")
    }

    fun removeDefaultHandler() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        huskyApp.unregisterActivityLifecycleCallbacks(activityCallbacks)
        lastActivity = null

        Timber.d("Remove default handler")
    }
}
