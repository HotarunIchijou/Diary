package org.kaorun.diary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedTaskDao {

    @Query("SELECT * FROM cached_tasks")
    suspend fun getAllTasks(): List<CachedTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<CachedTask>)

    @Query("DELETE FROM cached_tasks")
    suspend fun clearAll()
}
