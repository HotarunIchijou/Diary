package org.kaorun.diary.utils

import android.content.res.Resources

object ConvertUtils {
    fun Float.toPx(): Float = (this * Resources.getSystem().displayMetrics.density)
    fun Int.toPx(): Int = ((this * Resources.getSystem().displayMetrics.density).toInt())
}