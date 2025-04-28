package org.kaorun.diary.data

data class TasksDatabase(
    var id: String = "",
    val title: String = "",
    var isCompleted: Boolean = false,
    val time: String? = null,
    val date: String? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "isCompleted" to isCompleted,
        "time" to time,
        "date" to date
    )
}