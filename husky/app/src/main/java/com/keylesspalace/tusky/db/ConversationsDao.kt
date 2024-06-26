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

package com.keylesspalace.tusky.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.keylesspalace.tusky.components.conversation.ConversationEntity
import io.reactivex.Single

@Dao
interface ConversationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversations: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(conversation: ConversationEntity): Single<Long>

    @Delete
    fun delete(conversation: ConversationEntity): Single<Int>

    @Query(
        "SELECT * FROM ConversationEntity WHERE accountId = :accountId " +
            "ORDER BY s_createdAt DESC"
    )
    fun conversationsForAccount(accountId: Long): DataSource.Factory<Int, ConversationEntity>

    @Query("DELETE FROM ConversationEntity WHERE accountId = :accountId")
    fun deleteForAccount(accountId: Long)
}
