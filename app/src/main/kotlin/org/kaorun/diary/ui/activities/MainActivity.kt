package org.kaorun.diary.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.kaorun.diary.R
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.databinding.ActivityMainBinding
import org.kaorun.diary.ui.activities.settings.SettingsActivity
import org.kaorun.diary.ui.adapters.NotesAdapter
import org.kaorun.diary.ui.fragments.WelcomeFragment
import org.kaorun.diary.ui.managers.SearchHistoryManager
import org.kaorun.diary.ui.managers.SearchManager
import org.kaorun.diary.utils.ConvertUtils.toPx
import org.kaorun.diary.utils.InsetsHandler
import org.kaorun.diary.utils.LayoutMode
import org.kaorun.diary.utils.SpaceItemDecoration
import org.kaorun.diary.viewmodel.MainViewModel

class MainActivity : BaseActivity() {
	private lateinit var auth: FirebaseAuth
	private lateinit var databaseReference: DatabaseReference
	private lateinit var binding: ActivityMainBinding
	private lateinit var notesAdapter: NotesAdapter
	private lateinit var layoutManager: LayoutManager
	private lateinit var searchHistoryManager: SearchHistoryManager
	private val viewModel: MainViewModel by viewModels()
	private val notesList = mutableListOf<NotesDatabase>()
	private var backPressedCallback: OnBackPressedCallback? = null
    private var itemDecoration: RecyclerView.ItemDecoration? = null
    private var layoutMode = LayoutMode.LIST

    override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
        initBinding()
        initFirebase()
        checkNotificationPermission()
        setupUI()
        observeViewModel()
	}

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference("Notes")

        if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
            navigateToWelcomeFragment()
        } else {
            addShortcuts()
            showMainContent()
        }
    }

    private fun setupUI() {
        setupInsets()
        setupRecyclerView()
        setupScrollBehavior()
        setupSearchManager()
        setupSideSheetButton()
        setupSwitchLayoutButton()
        setupContextualToolbar()
        setupFab()
    }

    private fun setupRecyclerView() {
		notesAdapter = NotesAdapter(
            notesList,
            onItemClicked = { noteId, noteTitle, noteContent ->
                if (!notesAdapter.isSelectionModeActive) {
                    val intent = Intent(this, NoteActivity::class.java).apply {
                        putExtra("NOTE_ID", noteId)
                        putExtra("NOTE_TITLE", noteTitle)
                        putExtra("NOTE_CONTENT", noteContent)
                    }
                    startActivity(intent)
                }
            },
            onSelectionChanged = { isSelectionModeActive ->
                if (isSelectionModeActive) {
                    startActionModeAnimated()
                    binding.contextualToolbar.title =
                        notesAdapter.getSelectedNotes().size.toString()
                } else {
                    hideContextualToolbarAndClearSelection()
                }
            },
            onDeleteClicked = { noteId ->
                deleteNotes(listOf(noteId))
            }
        )

		layoutManager = StaggeredGridLayoutManager(
            layoutMode.spanCount,
            StaggeredGridLayoutManager.VERTICAL
        )
		binding.recyclerView.itemAnimator = DefaultItemAnimator()
		binding.recyclerView.apply {
			adapter = notesAdapter
			layoutManager = layoutManager
		}

        applyItemDecoration()
    }

	private fun setupScrollBehavior() {
		val fab = binding.fab
		binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)
				if (dy > 12 && fab.isShown) fab.hide()
				if (dy < -12 && !fab.isShown) fab.show()
			}
		})
	}

	private fun setupSearchManager() {
        searchHistoryManager = SearchHistoryManager(this, "notes")
		SearchManager(
			binding = binding,
			onBackPressedDispatcher = this.onBackPressedDispatcher,
			notesAdapter = notesAdapter,
			lifecycleOwner = this,
			notesList = notesList,
			backPressedCallback = backPressedCallback
		) {
			setupSideSheetButton()
		}
	}

	private fun setupSwitchLayoutButton() {
		with(binding.switchLayoutButton) {
			this.setOnClickListener {
				switchLayout()
				if (layoutMode == LayoutMode.LIST) this.setIconResource(R.drawable.grid_view_24px)
				else this.setIconResource(R.drawable.view_agenda_24px)
			}
		}
	}

	private fun setupSideSheetButton() {
		binding.sideSheetButton.icon = AppCompatResources.getDrawable(
			binding.mainActivity.context,
			R.drawable.menu_24px)

		binding.sideSheetButton.setOnClickListener {
			val sideSheetDialog = SideSheetDialog(this)

			with(sideSheetDialog) {
				setContentView(R.layout.side_sheet_layout)
				setFitsSystemWindows(false)
				show()
				setSheetEdge(Gravity.START)
			}

			val navigationView =
				sideSheetDialog.findViewById<NavigationView>(R.id.sideSheetNavigationView)
			InsetsHandler.applyViewInsets(navigationView!!)
			val notes = navigationView.menu.findItem(R.id.notes)
			notes?.isChecked = true

            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.notes -> {
                        sideSheetDialog.hide()
                    }

                    R.id.tasks -> {
						finish()
                        val intent = Intent(this, TasksMainActivity::class.java)
                        startActivity(intent)
                        sideSheetDialog.hide()
                    }

                    R.id.settings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                        sideSheetDialog.hide()
                    }

                    R.id.signOut -> {
                        FirebaseAuth.getInstance().signOut()
                        navigateToWelcomeFragment()
                        sideSheetDialog.hide()
                    }
                }
                true
            }
		}
	}

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyFabInsets(binding.fab)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }

    private fun setupContextualToolbar() {
        binding.contextualToolbar.apply {
            inflateMenu(R.menu.menu_select_appbar)
            setNavigationIcon(R.drawable.close_24px)
            setNavigationOnClickListener { hideContextualToolbarAndClearSelection() }

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        deleteNotes(notesAdapter.getSelectedNotes())
                        hideContextualToolbarAndClearSelection()
                        true
                    }
                    else -> false
                }
            }
        }
        InsetsHandler.applyAppBarInsets(binding.contextualToolbarContainer)
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, NoteActivity::class.java)
            startActivity(intent)
        }
    }

	private fun observeViewModel() {
		viewModel.isLoading.observe(this) {
			isLoading -> binding.loading.isVisible = isLoading
		}
		viewModel.notesList.observe(
			this
		) { notes ->
			notesList.clear()
			notesList.addAll(notes)
			notesAdapter.updateNotes(notes.toMutableList())
			binding.notesEmpty.notesEmptyLayout.isVisible = notes.isEmpty()
		}
	}

	private fun navigateToWelcomeFragment() {
		binding.recyclerView.visibility = View.GONE
		binding.searchBar.visibility = View.GONE
		binding.fab.visibility = View.GONE
		binding.fragmentContainerView.visibility = View.VISIBLE

		// Create the WelcomeFragment instance
		val welcomeFragment = WelcomeFragment()

		// Begin the fragment transaction
		supportFragmentManager.beginTransaction()
			.replace(R.id.fragmentContainerView, welcomeFragment)
			.commit()
	}

	private fun addShortcuts() {
		// Add Task shortcut
		val addTaskIntent = Intent()
		addTaskIntent.setClass(this, TaskAddActivity::class.java)
		addTaskIntent.action = "org.kaorun.diary.action.CREATE_TASK"
		addTaskIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

		val addTaskShortcut = ShortcutInfoCompat.Builder(this, "create_task")
			.setShortLabel(getString(R.string.add_task))
			.setIcon(IconCompat.createWithResource(this, R.drawable.ic_launcher_shortcut_tasks))
			.setIntent(addTaskIntent)
			.build()

		ShortcutManagerCompat.pushDynamicShortcut(this, addTaskShortcut)

		// Add Note shortcut
		val addNoteIntent = Intent()
		addNoteIntent.setClass(this, NoteActivity::class.java)
		addNoteIntent.action = "org.kaorun.diary.action.CREATE_NOTE"
		addNoteIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

		val addNoteShortcut = ShortcutInfoCompat.Builder(this, "create_note")
			.setShortLabel(getString(R.string.add_note))
			.setIcon(IconCompat.createWithResource(this, R.drawable.ic_launcher_shortcut_notes))
			.setIntent(addNoteIntent)
			.build()

		ShortcutManagerCompat.pushDynamicShortcut(this, addNoteShortcut)
	}

	private fun showMainContent() {
		binding.recyclerView.visibility = View.VISIBLE
		binding.searchBar.visibility = View.VISIBLE
		binding.fab.visibility = View.VISIBLE
		binding.fragmentContainerView.visibility = View.GONE
	}

	private fun switchLayout() {
        binding.recyclerView.layoutManager = layoutManager
		layoutMode = if (layoutMode == LayoutMode.LIST) LayoutMode.GRID
		else LayoutMode.LIST

        applyLayout()
	}

    private fun applyLayout() {
        binding.recyclerView.layoutManager =
            StaggeredGridLayoutManager(
                layoutMode.spanCount,
                StaggeredGridLayoutManager.VERTICAL
            )
        applyItemDecoration()
    }

    private fun checkNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
		}
	}

	private fun startActionModeAnimated() {
		binding.searchBar.expand(binding.contextualToolbarContainer, binding.appBarLayout)
		binding.contextualToolbar.title = notesAdapter.getSelectedNotes().size.toString()
	}

	private fun hideContextualToolbarAndClearSelection() {
		if (binding.searchBar.collapse(binding.contextualToolbarContainer, binding.appBarLayout)) {
			notesAdapter.clearSelection()
		}
	}

    private fun applyItemDecoration() {
        itemDecoration?.let { binding.recyclerView.removeItemDecoration(it) }

        val spanCount = layoutMode.spanCount
        val spacingPx = 8.toPx()

        itemDecoration = SpaceItemDecoration(spanCount, spacingPx)
        binding.recyclerView.addItemDecoration(itemDecoration!!)
    }

    private fun deleteNotes(noteIds: List<String>) {
        val deletedNotes = viewModel.deleteNotesTemporarily(noteIds)

        val text = if (noteIds.size == 1) {
            getString(R.string.note_deleted)
        } else {
            getString(R.string.notes_deleted)
        }

        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fab)
            .setAction(R.string.undo) {
                // Restore notes if user taps undo
                viewModel.restoreNotes(deletedNotes)
            }.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    // If user did not undo, delete from Database
                    if (event != DISMISS_EVENT_ACTION) {
                        viewModel.permanentlyDeleteNotes(deletedNotes)
                    }
                }
            }).show()
    }
}