package com.keylesspalace.tusky.di

import com.keylesspalace.tusky.components.conversation.ConversationsRepository
import com.keylesspalace.tusky.components.instance.InstanceRepository
import com.keylesspalace.tusky.components.report.adapter.StatusesRepository
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.repository.ChatRepository
import com.keylesspalace.tusky.repository.ChatRepositoryImpl
import com.keylesspalace.tusky.repository.TimelineRepository
import com.keylesspalace.tusky.repository.TimelineRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    factory {
        ChatRepositoryImpl(get<AppDatabase>().chatsDao(), get(), get(), get())
    } bind ChatRepository::class

    single {
        ConversationsRepository(get(), get())
    }

    single {
        StatusesRepository(get())
    }

    factory {
        TimelineRepositoryImpl(get<AppDatabase>().timelineDao(), get(), get(), get())
    } bind TimelineRepository::class

    single {
        InstanceRepository(get(), get(), get())
    }
}
