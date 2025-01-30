package com.keylesspalace.tusky.appstore

import com.keylesspalace.tusky.TabData
import com.keylesspalace.tusky.entity.Account
import com.keylesspalace.tusky.entity.ChatMessage
import com.keylesspalace.tusky.entity.Poll
import com.keylesspalace.tusky.entity.Status

data class FavoriteEvent(val statusId: String, val favourite: Boolean) : Dispatchable
data class ReblogEvent(val statusId: String, val reblog: Boolean) : Dispatchable
data class BookmarkEvent(val statusId: String, val bookmark: Boolean) : Dispatchable
data class MuteConversationEvent(val statusId: String, val mute: Boolean) : Dispatchable
data class EmojiReactEvent(val newStatus: Status) : Dispatchable
data class UnfollowEvent(val accountId: String) : Dispatchable
data class BlockEvent(val accountId: String) : Dispatchable
data class MuteEvent(val accountId: String, val mute: Boolean) : Dispatchable
data class StatusDeletedEvent(val statusId: String) : Dispatchable
data class StatusPreviewEvent(val status: Status) : Dispatchable
data class StatusComposedEvent(val status: Status) : Dispatchable
data class StatusScheduledEvent(val status: Status) : Dispatchable
data class ProfileEditedEvent(val newProfileData: Account) : Dispatchable
data class PreferenceChangedEvent(val preferenceKey: String) : Dispatchable
data class MainTabsChangedEvent(val newTabs: List<TabData>) : Dispatchable
data class PollVoteEvent(val statusId: String, val poll: Poll) : Dispatchable
data class DomainMuteEvent(val instance: String) : Dispatchable
data class ChatMessageDeliveredEvent(val chatMsg: ChatMessage) : Dispatchable
data class ChatMessageReceivedEvent(val chatMsg: ChatMessage) : Dispatchable
data class AnnouncementReadEvent(val announcementId: String) : Dispatchable
data class UnpinStatus(val status: Status) : Dispatchable
