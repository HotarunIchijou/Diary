package org.kaorun.diary.data

import android.app.Activity

data class SettingsItem(
    val title: String,
    val summary: String,
    val icon: Int?,
    val targetActivity: Class<out Activity>?,
    val url: String? = null
)

