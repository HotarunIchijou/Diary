package org.kaorun.diary.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.kaorun.diary.data.CachedTask
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.data.TasksRoomDatabase

object TasksLocalCache {
    fun cacheTasks(context: Context, tasks: List<TasksDatabase>) {
        runBlocking(Dispatchers.IO) {
            val cachedTasks = tasks.map {
                CachedTask(
                    id = it.id,
                    title = it.title,
                    date = it.date,
                    time = it.time,
                    isCompleted = it.isCompleted
                )
            }
            val db = TasksRoomDatabase.getDatabase(context)
            db.cachedTaskDao().clearAll()
            db.cachedTaskDao().insertAll(cachedTasks)
        }
    }

    // Получение закешированных задач из Room
    fun getCachedTasks(context: Context): List<TasksDatabase> {
        return runBlocking(Dispatchers.IO) {
            val db = TasksRoomDatabase.getDatabase(context)
            val cachedTasks = db.cachedTaskDao().getAllTasks()
            cachedTasks.map {
                TasksDatabase(
                    id = it.id,
                    title = it.title,
                    date = it.date,
                    time = it.time,
                    isCompleted = it.isCompleted
                )
            }
        }
    }
}
