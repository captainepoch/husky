/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky.entity

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.util.Date

data class PleromaNotification(
    @SerializedName("is_seen") val seen: Boolean
)

data class Notification(
    val type: Type,
    val id: String,
    val account: Account,
    val status: Status?,
    val pleroma: PleromaNotification? = null,
    val emoji: String? = null,
    @SerializedName("emoji_url") val emojiUrl: String? = null,
    @SerializedName("chat_message") val chatMessage: ChatMessage? = null,
    @SerializedName("created_at") val createdAt: Date? = null,
    val target: Account? = null
) {

    @JsonAdapter(NotificationTypeAdapter::class)
    enum class Type(val presentation: String) {
        UNKNOWN("unknown"),
        MENTION("mention"),
        REBLOG("reblog"),
        FAVOURITE("favourite"),
        FOLLOW("follow"),
        POLL("poll"),
        EMOJI_REACTION("pleroma:emoji_reaction"),
        FOLLOW_REQUEST("follow_request"),
        CHAT_MESSAGE("pleroma:chat_mention"),
        MOVE("move"),
        // Mastodon 3.3.0rc1
        STATUS("status"),
        UPDATE("update");

        companion object {

            @JvmStatic
            fun byString(s: String): Type {
                entries.forEach {
                    if (s == it.presentation) {
                        return it
                    }
                }
                return UNKNOWN
            }

            val asList = listOf(
                MENTION,
                REBLOG,
                FAVOURITE,
                FOLLOW,
                POLL,
                EMOJI_REACTION,
                FOLLOW_REQUEST,
                CHAT_MESSAGE,
                MOVE,
                STATUS,
                UPDATE
            )

            val asStringList = asList.map { it.presentation }
        }

        override fun toString(): String {
            return presentation
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Notification) {
            return false
        }
        val notification = other as Notification?
        return notification?.id == this.id
    }

    class NotificationTypeAdapter : JsonDeserializer<Type> {

        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: java.lang.reflect.Type,
            context: JsonDeserializationContext
        ): Type {
            return Type.byString(json.asString)
        }
    }

    companion object {

        // For Pleroma compatibility that uses Mention type
        @JvmStatic
        fun rewriteToStatusTypeIfNeeded(body: Notification, accountId: String): Notification {
            if (body.type == Type.MENTION && body.status != null) {
                return if (body.status.mentions.any {
                    it.id == accountId
                }
                ) {
                    body
                } else {
                    body.copy(type = Type.STATUS)
                }
            }
            return body
        }
    }
}
