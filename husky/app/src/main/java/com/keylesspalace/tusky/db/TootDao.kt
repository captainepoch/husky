package com.keylesspalace.tusky.db

import io.reactivex.Observable
import androidx.room.Dao
import androidx.room.Query

@Dao
interface TootDao {

    @Query("SELECT * FROM TootEntity ORDER BY uid DESC")
    fun loadAll(): List<TootEntity>

    @Query("DELETE FROM TootEntity WHERE uid = :uid")
    fun delete(uid: Int): Int

    @Query("SELECT * FROM TootEntity WHERE uid = :uid")
    fun find(uid: Int): TootEntity

    @Query("SELECT COUNT(*) FROM TootEntity")
    fun savedTootCount(): Observable<Int>
}
