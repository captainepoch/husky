/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2018  Levi Bard
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

package com.keylesspalace.tusky.components.instance.data.models.data

import com.google.gson.annotations.SerializedName
import com.keylesspalace.tusky.entity.Account

data class Instance(
    @SerializedName("uri") val uri: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("email") val email: String,
    @SerializedName("version") val version: String,
    @SerializedName("urls") val urls: Map<String, String>,
    @SerializedName("stats") val stats: Map<String, Int>?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("languages") val languages: List<String>,
    @SerializedName("contact_account") val contactAccount: Account,
    @SerializedName("max_toot_chars") val maxTootChars: Int?,
    @SerializedName("max_bio_chars") val maxBioChars: Int?,
    @SerializedName("poll_limits") val pollLimits: PollLimits?,
    @SerializedName("chat_limit") val chatLimit: Int?,
    @SerializedName("avatar_upload_limit") val avatarUploadLimit: Long?,
    @SerializedName("banner_upload_limit") val bannerUploadLimit: Long?,
    @SerializedName("description_limit") val descriptionLimit: Int?,
    @SerializedName("upload_limit") val uploadLimit: Long?,
    @SerializedName("pleroma") val pleroma: InstancePleroma?
) {
    override fun hashCode(): Int {
        return uri.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Instance) {
            return false
        }
        val instance = other as Instance?
        return instance?.uri.equals(uri)
    }
}
