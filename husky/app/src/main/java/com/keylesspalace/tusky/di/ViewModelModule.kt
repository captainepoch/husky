package com.keylesspalace.tusky.di

import com.keylesspalace.tusky.components.announcements.AnnouncementsViewModel
import com.keylesspalace.tusky.components.chat.ChatViewModel
import com.keylesspalace.tusky.components.compose.ComposeViewModel
import com.keylesspalace.tusky.components.conversation.ConversationsViewModel
import com.keylesspalace.tusky.components.drafts.DraftsViewModel
import com.keylesspalace.tusky.components.lists.account.ui.viewmodel.ListsForAccountViewModel
import com.keylesspalace.tusky.components.profile.ui.viewmodel.EditProfileViewModel
import com.keylesspalace.tusky.components.report.ReportViewModel
import com.keylesspalace.tusky.components.scheduled.ScheduledTootViewModel
import com.keylesspalace.tusky.components.search.SearchViewModel
import com.keylesspalace.tusky.view.emojireactions.CustomEmojiPickerViewModel
import com.keylesspalace.tusky.viewmodel.AccountViewModel
import com.keylesspalace.tusky.viewmodel.AccountsInListViewModel
import com.keylesspalace.tusky.viewmodel.ListsViewModel
import com.keylesspalace.tusky.viewmodel.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelsModule = module {
    viewModelOf(::AccountsInListViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::AnnouncementsViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::ComposeViewModel)
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::CustomEmojiPickerViewModel)
    viewModelOf(::DraftsViewModel)
    viewModelOf(::EditProfileViewModel)
    viewModelOf(::ListsViewModel)
    viewModelOf(::ListsForAccountViewModel)
    viewModelOf(::ReportViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::ScheduledTootViewModel)
    viewModelOf(::SplashViewModel)
}
