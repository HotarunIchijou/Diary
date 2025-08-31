package org.kaorun.diary.utils

import android.content.res.Resources

object ConvertUtils {
    fun Float.toPx(): Float = (this * Resources.getSystem().displayMetrics.density)
}