package org.kaorun.diary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tasks")
data class CachedTask(
    @PrimaryKey val id: String,
    val title: String,
    val date: String?,
    val time: String?,
    val isCompleted: Boolean
)
