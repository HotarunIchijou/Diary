package org.kaorun.diary.ui.managers

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.search.SearchView
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ActivityTasksMainBinding
import org.kaorun.diary.ui.adapters.SearchHistoryAdapter
import org.kaorun.diary.ui.adapters.TasksAdapter

class SearchTasksManager(
    private val binding: ActivityTasksMainBinding,
    private val onBackPressedDispatcher: OnBackPressedDispatcher,
    private val tasksAdapter: TasksAdapter,
    private val lifecycleOwner: LifecycleOwner,
    private var tasksList: MutableList<TasksDatabase>,
    private var backPressedCallback: OnBackPressedCallback? = null,
    private val restoreSideSheetListener: (() -> Unit)? = null
) {

    private lateinit var searchAdapter: SearchHistoryAdapter
    private lateinit var query: String
    private val searchBar = binding.searchBar
    private val searchView = binding.searchView
    private val recentSearches = mutableListOf<String>()
    private val tasksSearchHistoryManager = SearchHistoryManager(binding.root.context, "tasks")

    init {
        setupSearchAdapter()
        setupSearchBehavior()
    }

    private fun setupSearchBehavior() {
        recentSearches.addAll(tasksSearchHistoryManager.loadSearchHistory())
        updateSearchSuggestions()

        searchView.findViewById<RecyclerView>(R.id.SearchTasksRecyclerView).apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(context)
        }

        searchView.setupWithSearchBar(searchBar)

        searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING) {
                binding.fab.hide()
            } else if (newState == SearchView.TransitionState.HIDING) {
                binding.fab.show()
            }
        }

        searchView.editText.setOnEditorActionListener { p0, _, _ ->
            query = p0?.text.toString()
            if (query.isNotBlank()) {
                if (!recentSearches.contains(query)) {
                    recentSearches.add(0, query)
                    tasksSearchHistoryManager.saveSearchHistory(recentSearches)
                    updateSearchSuggestions()
                }
                searchView.hide()
                searchBar.setText(query)
                filterTasks(query)
            } else {
                searchView.hide()
                resetTasksList()
                searchBar.clearText()
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
                filterTasks(suggestion)
            },
            onItemDeleted = { suggestion ->
                recentSearches.remove(suggestion)
                updateSearchSuggestions()
                tasksSearchHistoryManager.saveSearchHistory(recentSearches)
            }
        )

        searchView.findViewById<RecyclerView>(R.id.SearchTasksRecyclerView).apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun updateSearchSuggestions() {
        searchAdapter.updateSuggestions(recentSearches)
        binding.searchSuggestionsEmpty.searchSuggestionsEmptyLayout.visibility =
            if (recentSearches.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filterTasks(query: String) {
        val filteredList = tasksList.filter {
            it.title.contains(query, ignoreCase = true)
        }.toMutableList()

        binding.tasksEmpty.tasksEmptyLayout.visibility = View.GONE
        binding.sideSheetButton.icon = AppCompatResources.getDrawable(
            binding.tasksMainActivity.context,
            R.drawable.arrow_back_24px
        )
        searchBar.textCentered = false
        backPressedCallback?.remove()
        binding.fab.hide()

        tasksAdapter.updateTasks(filteredList)

        if (filteredList.isEmpty()) {
            binding.nothingFoundTasks.nothingFoundTasksLayout.visibility = View.VISIBLE
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resetTasksList()
                searchBar.clearText()
                binding.fab.show()
                backPressedCallback?.remove()
                binding.nothingFoundTasks.nothingFoundTasksLayout.visibility = View.GONE
            }
        }

        onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback!!)

        binding.sideSheetButton.setOnClickListener {
            resetTasksList()
            searchBar.clearText()
            binding.fab.show()
            backPressedCallback?.remove()
            binding.nothingFoundTasks.nothingFoundTasksLayout.visibility = View.GONE
            restoreSideSheetListener?.invoke()
        }
    }

    private fun resetTasksList() {
        tasksAdapter.updateTasks(tasksList)
        backPressedCallback?.remove()
        searchBar.textCentered = true
        binding.nothingFoundTasks.nothingFoundTasksLayout.visibility = View.GONE
        binding.tasksEmpty.tasksEmptyLayout.visibility =
            if (tasksList.isNotEmpty()) View.GONE else View.VISIBLE

        updateSearchSuggestions()
    }
}
