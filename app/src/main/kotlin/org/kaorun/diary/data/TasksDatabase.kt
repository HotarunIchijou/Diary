package org.kaorun.diary.data

data class TasksDatabase(
    var id: String = "",
    val title: String = "",
    var isCompleted: Boolean = false,
    val time: String? = null,
    val date: String? = null
)
