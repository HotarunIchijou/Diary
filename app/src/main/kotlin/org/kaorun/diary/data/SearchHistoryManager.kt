package org.kaorun.diary.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SearchHistoryManager(context: Context, private val keyPrefix: String) {

	private val sharedPreferences: SharedPreferences =
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	companion object {
		private const val PREF_NAME = "SearchHistory"
	}

	private fun getKey(): String {
		return "${keyPrefix}_recentSearches"
	}

	fun saveSearchHistory(searches: List<String>) {
		sharedPreferences.edit {
			putStringSet(getKey(), searches.toSet())
		}
	}

	fun loadSearchHistory(): MutableList<String> {
		val searches = sharedPreferences.getStringSet(getKey(), emptySet())
		return searches?.toMutableList() ?: mutableListOf()
	}
}
