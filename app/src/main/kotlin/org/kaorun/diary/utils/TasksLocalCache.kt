package org.kaorun.diary.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.kaorun.diary.data.TasksDatabase
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object TasksLocalCache {

    private const val PREFS_NAME = "tasks_cache"
    private const val TASKS_KEY = "tasks"

    fun getCachedTasks(context: Context): List<TasksDatabase> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(TASKS_KEY, null)
        Log.d("TasksLocalCache", "Загружаем задачи из SharedPreferences: $json")
        return if (json != null) {
            val type = object : TypeToken<List<TasksDatabase>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun saveTasks(context: Context, tasks: List<TasksDatabase>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(tasks.map { task ->
            task.copy(date = task.date ?: getCurrentDate())  // Пример с установкой текущей даты, если она отсутствует
        })
        Log.d("TasksLocalCache", "Сохраняем задачи в SharedPreferences: $json")
        prefs.edit { putString(TASKS_KEY, json) }
    }

    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date())  // Возвращает текущую дату в формате "MMM d, yyyy"
    }

}
