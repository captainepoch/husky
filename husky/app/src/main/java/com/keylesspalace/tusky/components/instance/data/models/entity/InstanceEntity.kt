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

package com.keylesspalace.tusky.components.instance.data.models.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.keylesspalace.tusky.components.instance.data.models.data.InstanceInfo
import com.keylesspalace.tusky.core.utils.InstanceConstants
import com.keylesspalace.tusky.db.Converters
import com.keylesspalace.tusky.entity.Emoji

@Entity
@TypeConverters(Converters::class)
data class InstanceEntity(
    @field:PrimaryKey var instance: String,
    val emojiList: List<Emoji>?,
    val maximumTootCharacters: Int?,
    val maxPollOptions: Int?,
    val maxPollOptionLength: Int?,
    val maxBioLength: Int?,
    val maxBioFields: Int?,
    val version: String?,
    val chatLimit: Int?,
    val quotePosting: Boolean = false
) {

    fun toInstanceInfo(): InstanceInfo {
        return InstanceInfo(
            maxTootLength = maximumTootCharacters ?: InstanceConstants.DEFAULT_CHARACTER_LIMIT,
            maxBioLength = maxBioLength ?: InstanceConstants.DEFAULT_BIO_LENGTH,
            maxBioFields = maxBioFields ?: InstanceConstants.DEFAULT_BIO_MAX_FIELDS,
            quotePosting = quotePosting
        )
    }
}
