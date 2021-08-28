package com.keylesspalace.tusky.di

import com.google.gson.Gson
import com.keylesspalace.tusky.db.AccountManager
import com.keylesspalace.tusky.db.AppDatabase
import com.keylesspalace.tusky.network.MastodonApi
import com.keylesspalace.tusky.repository.ChatRepository
import com.keylesspalace.tusky.repository.ChatRepositoryImpl
import com.keylesspalace.tusky.repository.TimelineRepository
import com.keylesspalace.tusky.repository.TimelineRepositoryImpl
import dagger.Module
import dagger.Provides

@Module
class RepositoryModule {
    @Provides
    fun providesTimelineRepository(
            db: AppDatabase,
            mastodonApi: MastodonApi,
            accountManager: AccountManager,
            gson: Gson
    ): TimelineRepository {
        return TimelineRepositoryImpl(db.timelineDao(), mastodonApi, accountManager, gson)
    }

    @Provides
    fun providesChatRepository(
            db: AppDatabase,
            mastodonApi: MastodonApi,
            accountManager: AccountManager,
            gson: Gson
    ): ChatRepository {
        return ChatRepositoryImpl(db.chatsDao(), mastodonApi, accountManager, gson)
    }
}