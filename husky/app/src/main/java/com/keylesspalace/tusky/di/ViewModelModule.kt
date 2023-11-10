package com.keylesspalace.tusky.di

import com.keylesspalace.tusky.components.announcements.AnnouncementsViewModel
import com.keylesspalace.tusky.components.chat.ChatViewModel
import com.keylesspalace.tusky.components.compose.ComposeViewModel
import com.keylesspalace.tusky.components.conversation.ConversationsViewModel
import com.keylesspalace.tusky.components.drafts.DraftsViewModel
import com.keylesspalace.tusky.components.preference.AccountPreferencesViewModel
import com.keylesspalace.tusky.components.lists.account.ui.viewmodel.ListsForAccountViewModel
import com.keylesspalace.tusky.components.profile.ui.viewmodel.EditProfileViewModel
import com.keylesspalace.tusky.components.report.ReportViewModel
import com.keylesspalace.tusky.components.scheduled.ScheduledTootViewModel
import com.keylesspalace.tusky.components.search.SearchViewModel
import com.keylesspalace.tusky.viewmodel.AccountViewModel
import com.keylesspalace.tusky.viewmodel.AccountsInListViewModel
import com.keylesspalace.tusky.viewmodel.EditProfileViewModel
import com.keylesspalace.tusky.viewmodel.ListsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelsModule = module {
    viewModel {
        AccountsInListViewModel(get())
    }

    viewModel {
        AccountPreferencesViewModel()
    }

    viewModel {
        AccountViewModel(get(), get(), get())
    }

    viewModel {
        AnnouncementsViewModel(get(), get(), get(), get())
    }

    viewModel {
        ChatViewModel(get(), get(), get(), get(), get(), get())
    }

    viewModel {
        ComposeViewModel(get(), get(), get(), get(), get(), get(), get())
    }

    viewModel {
        ConversationsViewModel(get(), get(), get(), get())
    }

    viewModel {
        DraftsViewModel(get(), get(), get(), get())
    }

    viewModel {
        EditProfileViewModel(get(), get(), get(), get())
    }

    viewModel {
        ListsViewModel(get())
    }

    viewModel {
        ListsForAccountViewModel(get())
    }

    viewModel {
        ReportViewModel(get(), get(), get())
    }

    viewModel {
        SearchViewModel(get(), get(), get())
    }

    viewModel {
        ScheduledTootViewModel(get(), get())
    }
}
