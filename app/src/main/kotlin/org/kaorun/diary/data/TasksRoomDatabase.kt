package org.kaorun.diary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedTask::class], version = 1, exportSchema = false)
abstract class TasksRoomDatabase : RoomDatabase() {

    abstract fun cachedTaskDao(): CachedTaskDao

    companion object {
        @Volatile
        private var INSTANCE: TasksRoomDatabase? = null

        fun getDatabase(context: Context): TasksRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TasksRoomDatabase::class.java,
                    "tasks_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
