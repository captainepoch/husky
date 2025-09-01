/* Copyright 2018 Conny Duck
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.entity

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Emoji(
    @SerializedName("category")
    val category: String,
    @SerializedName("shortcode")
    val shortcode: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("static_url")
    val staticUrl: String,
    @SerializedName("visible_in_picker")
    val visibleInPicker: Boolean?
) : Parcelable

data class EmojiReaction(
    val category: String,
    val name: String,
    val count: Int,
    val me: Boolean,
    val url: String?,
    @SerializedName("static_url")
    val staticUrl: String?,
    val accounts: List<Account>? // only for emoji_reactions_by
)
