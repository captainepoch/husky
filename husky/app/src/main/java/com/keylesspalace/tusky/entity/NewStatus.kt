/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2021  The Husky Developers
 * Copyright (C) 2019  Tusky Contributors
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

import com.google.gson.annotations.SerializedName

data class NewStatus(
    val status: String,
    @SerializedName("spoiler_text")
    val warningText: String,
    @SerializedName("in_reply_to_id")
    val inReplyToId: String?,
    val visibility: String,
    val sensitive: Boolean,
    @SerializedName("media_ids")
    val mediaIds: List<String>?,
    @SerializedName("scheduled_at")
    val scheduledAt: String?,
    @SerializedName("expires_in")
    val expiresIn: Int?,
    val poll: NewPoll?,
    @SerializedName("content_type")
    var contentType: String?,
    val preview: Boolean?,
    @SerializedName("quote_id")
    val quoteId: String?
)
