package org.kaorun.diary.data

data class TasksDatabase(
    var id: String,
    val title: String,
    var isCompleted: Boolean,
    val time: String?,
    // val createdAt: Long = System.currentTimeMillis()
) {
    // Firebase requires a no-arg constructor
    constructor() : this("", "", false, null)
}
