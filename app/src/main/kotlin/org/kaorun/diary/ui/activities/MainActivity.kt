package org.kaorun.diary.ui.activities

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.kaorun.diary.viewmodel.MainViewModel
import org.kaorun.diary.R
import org.kaorun.diary.ui.adapters.NotesAdapter
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.data.SearchHistoryManager
import org.kaorun.diary.databinding.ActivityMainBinding
import org.kaorun.diary.ui.managers.SearchManager
import org.kaorun.diary.ui.utils.InsetsHandler

class MainActivity : AppCompatActivity() {

	private lateinit var auth: FirebaseAuth
	private lateinit var databaseReference: DatabaseReference
	private lateinit var binding: ActivityMainBinding
	private lateinit var notesAdapter: NotesAdapter
	private lateinit var layoutManager: LayoutManager
	private lateinit var searchHistoryManager: SearchHistoryManager
	private val viewModel: MainViewModel by viewModels()
	private val notesList = mutableListOf<NotesDatabase>()
	private var actionMode: ActionMode? = null
	private var backPressedCallback: OnBackPressedCallback? = null
	private var isGridLayout = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		searchHistoryManager = SearchHistoryManager(this)

		setupInsets()
		setupRecyclerView()
		setupScrollBehavior()
		setupSearchManager()

		auth = FirebaseAuth.getInstance()
		databaseReference = FirebaseDatabase.getInstance().getReference("Notes")

		observeViewModel()

		binding.extendedFab.setOnClickListener {
			val intent = Intent(this, NoteActivity::class.java)
			startActivity(intent)
		}
	}

	private fun setupRecyclerView() {
		notesAdapter = NotesAdapter(
			notesList,
			onItemClicked = { noteId, noteContent ->
				if (actionMode == null) {
					// Open note if not in selection mode
					val intent = Intent(this, NoteActivity::class.java).apply {
						putExtra("NOTE_ID", noteId)
						putExtra("NOTE_CONTENT", noteContent)
					}
					startActivity(intent)
				}
			},
			onSelectionChanged = { isSelectionModeActive ->
				if (isSelectionModeActive) {
					startActionMode()
				} else {
					binding.appBarLayout.visibility = View.VISIBLE
					actionMode?.finish()
				}
			}
		)

		layoutManager = LinearLayoutManager(this)
		binding.recyclerView.itemAnimator = DefaultItemAnimator()

		binding.recyclerView.apply {
			adapter = notesAdapter
			layoutManager = layoutManager
		}
	}

	private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			// No need to handle drag and drop for this use case
			return false
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			val position = viewHolder.adapterPosition
			notesAdapter.removeItem(position)
		}

		override fun onChildDraw(
			c: Canvas,
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			dX: Float,
			dY: Float,
			actionState: Int,
			isCurrentlyActive: Boolean
		) {
			// Optional: Customize swipe animation (background, icon, etc.)
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
		}
	}

	/* val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
	itemTouchHelper.attachToRecyclerView(binding.recyclerView) */

	private val actionModeCallback = object : ActionMode.Callback {
		override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
			menuInflater.inflate(R.menu.menu_select_appbar, menu)
			// Hide the AppBarLayout when ActionMode starts
			binding.appBarLayout.visibility = View.INVISIBLE
			return true
		}

		override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

		override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
			return when (item.itemId) {
				R.id.delete -> {
					viewModel.deleteNotes(notesAdapter.getSelectedNotes())
					mode.finish()
					true
				}

				else -> false
			}
		}


		override fun onDestroyActionMode(mode: ActionMode) {
			actionMode = null
			notesAdapter.clearSelection()
			// Show the AppBarLayout when ActionMode is destroyed
			binding.appBarLayout.visibility = View.VISIBLE
		}
	}

	private fun startActionMode() {
	if (actionMode == null) {

		actionMode = startSupportActionMode(actionModeCallback)
	}
}

	private fun setupScrollBehavior() {
		val fab = binding.extendedFab
		binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				super.onScrolled(recyclerView, dx, dy)

				if (dy > 12 && fab.isExtended) fab.shrink()
				if (dy < -12 && !fab.isExtended) fab.extend()
				if (!recyclerView.canScrollVertically(-1)) fab.extend()
			}
		})
	}

	private fun setupSearchManager() {
		SearchManager(
			binding = binding,
			onBackPressedDispatcher = this.onBackPressedDispatcher,
			notesAdapter = notesAdapter,
			lifecycleOwner = this,
			layoutManager = layoutManager,
			notesList = notesList,
			backPressedCallback = backPressedCallback,
			isGridLayout = isGridLayout)
	}

	private fun observeViewModel() {
		viewModel.notesList.observe(
			this
		) { notes ->
			notesList.clear()
			notesList.addAll(notes)
			notesAdapter.updateNotes(notes.toMutableList())
		}
	}

	private fun setupInsets() {
		InsetsHandler.applyRecyclerViewInsets(binding.recyclerView)
		InsetsHandler.applyFabInsets(binding.extendedFab)
		InsetsHandler.applyAppBarInsets(binding.appBarLayout)
	}

	override fun onStart() {
		super.onStart()

		if (auth.currentUser == null) {
			startActivity(Intent(this, LoginActivity::class.java))
			finish()
		}
	}
}

