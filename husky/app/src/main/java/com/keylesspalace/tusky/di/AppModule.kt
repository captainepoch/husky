/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
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

import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.WorkerFactory
import com.keylesspalace.tusky.appstore.EventHub
import com.keylesspalace.tusky.appstore.EventHubImpl
import com.keylesspalace.tusky.components.notifications.NotificationFetcher
import com.keylesspalace.tusky.components.notifications.NotificationWorkerFactory
import com.keylesspalace.tusky.components.notifications.Notifier
import com.keylesspalace.tusky.components.notifications.SystemNotifier
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.util.LocaleManager
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    factory {
        LocalBroadcastManager.getInstance(get())
    } bind LocalBroadcastManager::class

    single {
        EventHubImpl
    } bind EventHub::class

    single {
        LocaleManager()
    }

    single {
        NotificationFetcher(get(), get(), get())
    }

    single {
        NotificationWorkerFactory(get())
    } bind WorkerFactory::class

    single {
        PreferenceManager.getDefaultSharedPreferences(get())
    } bind SharedPreferences::class

    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "tuskyDB")
            .allowMainThreadQueries()
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_10_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20,
                AppDatabase.MIGRATION_20_21,
                AppDatabase.MIGRATION_21_22,
                AppDatabase.MIGRATION_22_23,
                AppDatabase.MIGRATION_23_24,
                AppDatabase.MIGRATION_24_25,
                AppDatabase.MIGRATION_25_26,
                AppDatabase.MIGRATION_26_27,
                AppDatabase.MIGRATION_27_28,
                AppDatabase.MIGRATION_28_29,
                AppDatabase.MIGRATION_29_30,
                AppDatabase.MIGRATION_30_31,
                AppDatabase.MIGRATION_31_32,
                AppDatabase.MIGRATION_32_33,
                AppDatabase.MIGRATION_33_34
            ).build()
    } bind AppDatabase::class

    single {
        SystemNotifier(get())
    } bind Notifier::class
}
