package org.kaorun.diary.data

data class TasksDatabase(
    var id: String,
    val title: String,
    var isCompleted: Boolean,
    val time: String?,
    val date: String?
) {
    constructor() : this("", "", false, null, null)
}
