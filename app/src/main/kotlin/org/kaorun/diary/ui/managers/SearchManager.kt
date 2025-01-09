package org.kaorun.diary.ui.managers

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.search.SearchView
import org.kaorun.diary.R
import org.kaorun.diary.ui.adapters.NotesAdapter
import org.kaorun.diary.ui.adapters.SearchHistoryAdapter
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.data.SearchHistoryManager
import org.kaorun.diary.databinding.ActivityMainBinding

class SearchManager(
    private val binding: ActivityMainBinding,
    private val onBackPressedDispatcher: OnBackPressedDispatcher,
    private val notesAdapter: NotesAdapter,
    private val lifecycleOwner: LifecycleOwner,
    private var layoutManager: LayoutManager,
    private var notesList: MutableList<NotesDatabase>,
    private var backPressedCallback: OnBackPressedCallback? = null,
    private var isGridLayout: Boolean,
) {

	private lateinit var searchAdapter: SearchHistoryAdapter
	private lateinit var query: String
	private val searchBar = binding.searchBar
	private val searchView = binding.searchView
	private val recentSearches = mutableListOf<String>()
	private val searchHistoryManager = SearchHistoryManager(binding.mainActivity.context)

	init {
		setupSearchAdapter()
	    setupSearchBehavior()
	}

	private fun setupSearchBehavior() {

		recentSearches.addAll(searchHistoryManager.loadSearchHistory())
		searchAdapter.updateSuggestions(recentSearches)

		searchView.findViewById<RecyclerView>(R.id.SearchRecyclerView).apply {
			adapter = searchAdapter
			layoutManager = LinearLayoutManager(context)
		}

		searchView.setupWithSearchBar(searchBar)


		searchView.addTransitionListener { _, _, newState ->
			if (newState == SearchView.TransitionState.SHOWING) {
				binding.extendedFab.hide()
			} else if (newState == SearchView.TransitionState.HIDING) {
				binding.extendedFab.show()
			}
		}

		searchView.editText.setOnEditorActionListener { p0, _, _ ->
			query = p0?.text.toString()
			if (query.isNotBlank()) {
				if (!recentSearches.contains(query)) {
					recentSearches.add(0, query)
					searchHistoryManager.saveSearchHistory(recentSearches) // Save the new query
					searchAdapter.updateSuggestions(recentSearches) // Notify adapter
				}
				searchView.hide()
				searchBar.setText(query)
				filterNotes(query)
			} else {
				searchView.hide()
				resetNotesList()
				searchBar.clearText()
			}
			true
		}

		searchBar.inflateMenu(R.menu.menu_search_bar)

		searchBar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.layoutSwitcher -> {
					switchLayout()
					if (isGridLayout) it.setIcon(R.drawable.view_agenda_24px)
					else it.setIcon(R.drawable.grid_view_24px)
				}
			}
			true
		}
	}

	private fun setupSearchAdapter() {
		searchAdapter = SearchHistoryAdapter(
			recentSearches.toMutableList(),
			onItemClicked = { suggestion ->
				searchView.hide()
				searchBar.setText(suggestion)
				filterNotes(suggestion)
			},
			onItemDeleted = { suggestion ->
				recentSearches.remove(suggestion)
				searchAdapter.updateSuggestions(recentSearches)
				searchHistoryManager.saveSearchHistory(recentSearches)
			}
		)

		searchView.findViewById<RecyclerView>(R.id.SearchRecyclerView).apply {
			adapter = searchAdapter
			layoutManager = LinearLayoutManager(context)
		}
	}

	private fun filterNotes(query: String) {
		val filteredList = notesList.filter {
			it.title.contains(query, ignoreCase = true) // Filter notes by title
		}.toMutableList()
		searchBar.navigationIcon = AppCompatResources.getDrawable(binding.mainActivity.context, R.drawable.arrow_back_24px)
		backPressedCallback?.remove()
		binding.extendedFab.hide()
		notesAdapter.updateNotes(filteredList) // Update RecyclerView with filtered list
		if (filteredList.isEmpty()){
			binding.nothingFound.nothingFoundLayout.visibility= View.VISIBLE
		}


		// Handle back button for search mode
		backPressedCallback = object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				resetNotesList()
				searchBar.clearText()
				binding.extendedFab.show()
				backPressedCallback?.remove()
				binding.nothingFound.nothingFoundLayout.visibility= View.GONE
			}
		}

		onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback!!)

		// Set up navigation icon click listener to exit search mode
		searchBar.setNavigationOnClickListener {
			resetNotesList()
			searchBar.clearText()
			binding.extendedFab.show()
			backPressedCallback?.remove()
			searchBar.setNavigationOnClickListener(null)
			binding.nothingFound.nothingFoundLayout.visibility= View.GONE
		}
	}

	private fun resetNotesList() {
		searchBar.navigationIcon = AppCompatResources.getDrawable(binding.mainActivity.context, R.drawable.search_24px)
		notesAdapter.updateNotes(notesList) // Reset RecyclerView to show all notes
		backPressedCallback?.remove()
	}

	private fun switchLayout() {
		layoutManager = if (isGridLayout) {
			// Switch to LinearLayoutManager
			LinearLayoutManager(binding.mainActivity.context)
		} else {
			// Switch to GridLayoutManager (2 columns)
			GridLayoutManager(binding.mainActivity.context, 2)
		}

		binding.recyclerView.layoutManager = layoutManager
		isGridLayout = !isGridLayout
	}
}
