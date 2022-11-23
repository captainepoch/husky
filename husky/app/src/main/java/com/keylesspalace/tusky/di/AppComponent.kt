/* Copyright 2018 charlag
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.di

import com.keylesspalace.tusky.appstore.CacheUpdater
import com.keylesspalace.tusky.components.common.MediaUploader
import com.keylesspalace.tusky.components.common.MediaUploaderImpl
import com.keylesspalace.tusky.components.drafts.DraftHelper
import com.keylesspalace.tusky.core.logging.CrashHandler
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.util.SaveTootHelper
import org.koin.dsl.bind
import org.koin.dsl.module

val appComponentModule = module {
    single { CacheUpdater(get(), get(), get(), get()) }

    single { CrashHandler(get()) }

    single { AccountManager(get()) }

    factory {
        MediaUploaderImpl(get(), get())
    } bind MediaUploader::class

    factory {
        DraftHelper(get(), get())
    }

    factory {
        SaveTootHelper(get(), get())
    }
}
