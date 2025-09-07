/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  charlag
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

package com.keylesspalace.tusky.network

import com.keylesspalace.tusky.db.AccountManager
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class InstanceSwitchAuthInterceptor(
    private val accountManager: AccountManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.url.host == MastodonApi.PLACEHOLDER_DOMAIN) {
            val activeAccount = accountManager.activeAccount
            val builder = originalRequest.newBuilder()
            val instanceHeader = originalRequest.header(MastodonApi.DOMAIN_HEADER)
            if (instanceHeader != null) {
                builder.url(swapHost(originalRequest.url, instanceHeader))
                    .removeHeader(MastodonApi.DOMAIN_HEADER)
            } else if (activeAccount != null) {
                builder.url(swapHost(originalRequest.url, activeAccount.domain))
                    .header(
                        "Authorization",
                        String.format("Bearer %s", activeAccount.accessToken)
                    )
            }

            chain.proceed(builder.build())
        } else {
            chain.proceed(originalRequest)
        }
    }

    private fun swapHost(url: HttpUrl, host: String): HttpUrl {
        return url.newBuilder().host(host).build()
    }
}
