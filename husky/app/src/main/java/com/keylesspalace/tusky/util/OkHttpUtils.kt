/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
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

package com.keylesspalace.tusky.util

import android.content.Context
import android.os.Build.VERSION
import androidx.preference.PreferenceManager
import com.keylesspalace.tusky.BuildConfig
import com.keylesspalace.tusky.core.utils.ApplicationUtils.isDebug
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit.SECONDS

object OkHttpUtils {

    fun getCompatibleClientBuilder(context: Context): OkHttpClient.Builder {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val httpProxyEnabled = preferences.getBoolean("httpProxyEnabled", false)
        val httpServer = preferences.getString("httpProxyServer", "") ?: ""
        val httpPort = kotlin.runCatching {
            preferences.getString("httpProxyPort", "-1") ?: "-1"
        }.map {
            it.toInt()
        }.getOrElse { -1 }

        val cacheSize: Long = 25 * (1024 * 1024)

        val builder = OkHttpClient.Builder()
            .addInterceptor(getUserAgentInterceptor())
            // .addInterceptor(getDebugInformation())
            .addInterceptor(BrotliInterceptor)
            .readTimeout(60, SECONDS)
            .writeTimeout(60, SECONDS)
            .cache(Cache(context.cacheDir, cacheSize))

        if (isDebug()) {
            builder.addInterceptor(getDebugInformation())
        }

        if (httpProxyEnabled && httpServer.isNotEmpty() && httpPort in 0..65535) {
            builder.proxy(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(httpServer, httpPort)
                )
            )
        }

        return builder
    }

    /**
     * Add a custom User-Agent that contains Tusky & Android Version to all requests
     * Example:
     * User-Agent: Tusky/1.1.2 Android/5.0.2
     */
    private fun getUserAgentInterceptor(): Interceptor {
        return Interceptor { chain: Chain ->
            val requestWithUserAgent = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "${BuildConfig.APPLICATION_NAME}/${BuildConfig.VERSION_NAME} Android/${VERSION.RELEASE}"
                )
                .build()
            chain.proceed(requestWithUserAgent)
        }
    }

    private fun getDebugInformation(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = BODY
        }
    }
}
