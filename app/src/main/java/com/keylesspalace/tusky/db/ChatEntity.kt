package com.keylesspalace.tusky.db

import androidx.room.*

@Entity(
        primaryKeys = ["localId", "chatId"]
)
data class ChatEntity (
        val localId: Long, /* our user account id */
        val chatId: String,
        val accountId: String,
        val unread: Long,
        val updatedAt: Long,
        val lastMessageId: String?
)

data class ChatEntityWithAccount (
        @Embedded val chat: ChatEntity,
        @Embedded(prefix = "a_") val account: TimelineAccountEntity?,
        @Embedded(prefix = "msg_") val lastMessage: ChatMessageEntity? = null
)