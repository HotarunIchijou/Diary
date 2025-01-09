package org.kaorun.diary.data

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {

	private val sharedPreferences: SharedPreferences =
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	companion object {
		private const val PREF_NAME = "SearchHistory"
		private const val KEY_RECENT_SEARCHES = "recentSearches"
	}

	fun saveSearchHistory(searches: List<String>) {
		sharedPreferences.edit()
			.putStringSet(KEY_RECENT_SEARCHES, searches.toSet())
			.apply()
	}

	fun loadSearchHistory(): MutableList<String> {
		val searches = sharedPreferences.getStringSet(KEY_RECENT_SEARCHES, emptySet())
		return searches?.toMutableList() ?: mutableListOf()
	}
}
