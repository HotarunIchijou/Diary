package org.kaorun.diary.utils

import android.content.Context
import org.kaorun.diary.R
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    fun formatDate(context: Context, dateString: String?): String? {
        if (dateString == null) return null

        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val today = LocalDate.now()

        val taskDate = runCatching {
            MonthDay.parse(dateString, formatter).atYear(today.year).let {
                if (it.isBefore(today.minusDays(1))) it.plusYears(1) else it
            }
        }.getOrNull() ?: return dateString

        return when (taskDate) {
            today -> null
            today.minusDays(1) -> context.getString(R.string.date_yesterday)
            today.plusDays(1) -> context.getString(R.string.date_tomorrow)
            else -> taskDate.format(formatter)
        }
    }
}