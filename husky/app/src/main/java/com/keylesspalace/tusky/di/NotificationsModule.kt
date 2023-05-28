package com.keylesspalace.tusky.di

import androidx.work.WorkerFactory
import com.keylesspalace.tusky.components.unifiedpush.NotificationFetcher
import com.keylesspalace.tusky.components.unifiedpush.NotificationWorkerFactory
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationsModule = module {
    single {
        NotificationFetcher(get(), get(), get())
    }

    single {
        NotificationWorkerFactory(get())
    } bind WorkerFactory::class
}
