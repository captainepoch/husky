package com.keylesspalace.tusky.db

import androidx.room.Entity

/*
 * ChatMessage model
 */

@Entity(
        primaryKeys = ["localId", "messageId"]
)
data class ChatMessageEntity(
        val localId: Long,
        val messageId: String,
        val content: String?,
        val chatId: String,
        val accountId: String,
        val createdAt: Long,
        val attachment: String?,
        val emojis: String
)