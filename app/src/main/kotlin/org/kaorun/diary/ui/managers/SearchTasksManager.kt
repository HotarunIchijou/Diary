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
import org.kaorun.diary.data.SearchHistoryManager
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
        searchAdapter.updateSuggestions(recentSearches)

        searchView.findViewById<RecyclerView>(R.id.SearchTasksRecyclerView).apply {
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
                    tasksSearchHistoryManager.saveSearchHistory(recentSearches)
                    searchAdapter.updateSuggestions(recentSearches)
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

        searchBar.inflateMenu(R.menu.menu_search_bar)
        val menuItem = binding.searchBar.menu.findItem(R.id.layoutSwitcher)
        menuItem?.isVisible = tasksList.isNotEmpty()
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
                searchAdapter.updateSuggestions(recentSearches)
                tasksSearchHistoryManager.saveSearchHistory(recentSearches)
            }
        )

        searchView.findViewById<RecyclerView>(R.id.SearchTasksRecyclerView).apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun filterTasks(query: String) {
        val filteredList = tasksList.filter {
            it.title.contains(query, ignoreCase = true)
        }.toMutableList()

        searchBar.navigationIcon = AppCompatResources.getDrawable(binding.root.context, R.drawable.arrow_back_24px)
        backPressedCallback?.remove()
        binding.extendedFab.hide()
        tasksAdapter.updateTasks(filteredList)
        binding.tasksEmpty.tasksEmptyLayout.visibility = View.GONE

        if (filteredList.isEmpty()) {
            binding.nothingFound.nothingFoundLayout.visibility = View.VISIBLE
            binding.searchBar.menu.findItem(R.id.layoutSwitcher)?.isVisible = false
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                resetTasksList()
                searchBar.clearText()
                binding.extendedFab.show()
                backPressedCallback?.remove()
                binding.nothingFound.nothingFoundLayout.visibility = View.GONE
            }
        }
        onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback!!)

        searchBar.setNavigationOnClickListener {
            resetTasksList()
            searchBar.clearText()
            binding.extendedFab.show()
            backPressedCallback?.remove()
            searchBar.setNavigationOnClickListener(null)
            binding.nothingFound.nothingFoundLayout.visibility = View.GONE
        }
    }

    private fun resetTasksList() {
        searchBar.navigationIcon = AppCompatResources.getDrawable(binding.root.context, R.drawable.search_24px)
        tasksAdapter.updateTasks(tasksList)
        backPressedCallback?.remove()
        binding.searchBar.menu.findItem(R.id.layoutSwitcher)?.isVisible = tasksList.isNotEmpty()
        binding.tasksEmpty.tasksEmptyLayout.visibility = if (tasksList.isNotEmpty()) View.GONE else View.VISIBLE
    }
}
