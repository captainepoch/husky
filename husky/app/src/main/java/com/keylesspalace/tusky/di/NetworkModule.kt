/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2018  charlag
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

package com.keylesspalace.tusky.di

import android.content.Context
import android.text.Spanned
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.json.SpannedTypeAdapter
import com.keylesspalace.tusky.network.InstanceSwitchAuthInterceptor
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.network.MastodonService
import com.keylesspalace.tusky.util.OkHttpUtils
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun providesGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Spanned::class.java, SpannedTypeAdapter())
            .create()
    }

    @Provides
    @Singleton
    fun providesHttpClient(accountManager: AccountManager, context: Context): OkHttpClient {
        return OkHttpUtils.getCompatibleClientBuilder(context)
            .apply {
                addInterceptor(InstanceSwitchAuthInterceptor(accountManager))
            }
            .build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(httpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder().baseUrl("https://" + MastodonApi.PLACEHOLDER_DOMAIN)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .build()
    }

    @Provides
    @Singleton
    fun providesService(retrofit: Retrofit): MastodonApi {
        return MastodonService(retrofit)
    }
}
