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

import android.text.Spanned
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.keylesspalace.tusky.appstore.CacheUpdater
import com.keylesspalace.tusky.components.common.MediaUploader
import com.keylesspalace.tusky.components.common.MediaUploaderImpl
import com.keylesspalace.tusky.components.drafts.DraftHelper
import com.keylesspalace.tusky.core.logging.CrashHandler
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.json.SpannedTypeAdapter
import com.keylesspalace.tusky.util.SaveTootHelper
import org.koin.dsl.bind
import org.koin.dsl.module

val appComponentModule = module {
    factory {
        DraftHelper(get(), get())
    }

    factory {
        MediaUploaderImpl(get(), get())
    } bind MediaUploader::class

    factory {
        SaveTootHelper(get(), get())
    }

    single {
        AccountManager(get())
    }

    single {
        CacheUpdater(get(), get(), get(), get())
    }

    single {
        CrashHandler(get())
    }

    single {
        GsonBuilder()
            .registerTypeAdapter(Spanned::class.java, SpannedTypeAdapter())
            .create()
    } bind Gson::class
}
