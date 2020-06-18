package com.isaiahvonrundstedt.fokus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.isaiahvonrundstedt.fokus.features.history.History

@Dao
interface HistoryDAO {

    @Insert
    suspend fun insert(history: History)

    @Delete
    suspend fun remove(history: History)

    @Update
    suspend fun update(history: History)

    @Query("SELECT * FROM notifications")
    fun fetch(): LiveData<List<History>>

    @Query("DELETE FROM notifications")
    suspend fun clear()

}