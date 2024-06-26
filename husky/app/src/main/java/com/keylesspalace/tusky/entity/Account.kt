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

import android.text.Spanned
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.keylesspalace.tusky.json.CustomDateTypeAdapter
import java.util.Date

data class Account(
    val id: String,
    @SerializedName("username") val localUsername: String,
    @SerializedName("acct") val username: String,
    @SerializedName("display_name") val displayName: String?, // should never be null per Api definition, but some servers break the contract
    val note: Spanned,
    val url: String,
    val avatar: String,
    val header: String,
    val locked: Boolean = false,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("statuses_count") val statusesCount: Int = 0,
    val source: AccountSource? = null,
    val bot: Boolean = false,
    val emojis: List<Emoji>? = emptyList(), // nullable for backward compatibility
    val fields: List<Field>? = emptyList(), // nullable for backward compatibility
    val moved: Account? = null,
    val pleroma: PleromaAccount? = null
) {

    val name: String
        get() = if (displayName.isNullOrEmpty()) {
            localUsername
        } else {
            displayName
        }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Account) {
            return false
        }
        return other.id == this.id
    }

    fun deepEquals(other: Account): Boolean {
        return id == other.id &&
            localUsername == other.localUsername &&
            displayName == other.displayName &&
            note == other.note &&
            url == other.url &&
            avatar == other.avatar &&
            header == other.header &&
            locked == other.locked &&
            followersCount == other.followersCount &&
            followingCount == other.followingCount &&
            statusesCount == other.statusesCount &&
            source == other.source &&
            bot == other.bot &&
            emojis == other.emojis &&
            fields == other.fields &&
            moved == other.moved &&
            pleroma == other.pleroma
    }

    fun isRemote(): Boolean = this.username != this.localUsername
}

data class AccountSource(
    val privacy: Status.Visibility,
    val sensitive: Boolean,
    val note: String,
    val fields: List<StringField>?
)

data class Field(
    val name: String,
    val value: Spanned,
    @JsonAdapter(CustomDateTypeAdapter::class)
    @SerializedName("verified_at")
    val verifiedAt: Date? = null
)

data class StringField(
    val name: String,
    val value: String
)

data class PleromaAccount(
    @SerializedName("ap_id") val apId: String? = null,
    @SerializedName("accepts_chat_messages") val acceptsChatMessages: Boolean? = null,
    @SerializedName("is_moderator") val isModerator: Boolean? = null,
    @SerializedName("is_admin") val isAdmin: Boolean? = null
)
