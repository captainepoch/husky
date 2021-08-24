/* Copyright 2020 Alibek Omarov
 *
 * This file is a part of Husky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Husky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Husky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.entity

import android.text.Spanned
import com.google.gson.annotations.SerializedName
import java.util.*

data class ChatMessage(
        val id: String,
        val content: Spanned?,
        @SerializedName("chat_id") val chatId: String,
        @SerializedName("account_id") val accountId: String,
        @SerializedName("created_at") val createdAt: Date,
        val attachment: Attachment?,
        val emojis: List<Emoji>,
        val card: Card?
)

data class Chat(
    val account: Account,
    val id: String,
    val unread: Long,
    @SerializedName("last_message") val lastMessage: ChatMessage?,
    @SerializedName("updated_at") val updatedAt: Date
)

data class NewChatMessage(
    val content: String,
    val media_id: String?
)