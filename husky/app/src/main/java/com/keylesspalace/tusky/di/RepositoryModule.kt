package com.keylesspalace.tusky.di

import androidx.work.WorkerFactory
import com.keylesspalace.tusky.components.conversation.ConversationsRepository
import com.keylesspalace.tusky.components.notifications.NotificationFetcher
import com.keylesspalace.tusky.components.notifications.NotificationWorkerFactory
import com.keylesspalace.tusky.components.report.adapter.StatusesRepository
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.repository.ChatRepository
import com.keylesspalace.tusky.repository.ChatRepositoryImpl
import com.keylesspalace.tusky.repository.TimelineRepository
import com.keylesspalace.tusky.repository.TimelineRepositoryImpl
import com.keylesspalace.tusky.util.LocaleManager
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single {
        LocaleManager()
    }
    single {
        StatusesRepository(get())
    }
    factory {
        TimelineRepositoryImpl(get<AppDatabase>().timelineDao(), get(), get(), get())
    } bind TimelineRepository::class

    factory {
        ChatRepositoryImpl(get<AppDatabase>().chatsDao(), get(), get(), get())
    } bind ChatRepository::class

    single {
        ConversationsRepository(get(), get())
    }

    single {
        NotificationFetcher(get(), get(), get())
    }

    single {
        NotificationWorkerFactory(get())
    } bind WorkerFactory::class
}
