package org.kaorun.diary.ui.activities

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.kaorun.diary.R
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.data.SearchHistoryManager
import org.kaorun.diary.databinding.ActivityMainBinding
import org.kaorun.diary.ui.adapters.NotesAdapter
import org.kaorun.diary.ui.fragments.WelcomeFragment
import org.kaorun.diary.ui.managers.SearchManager
import org.kaorun.diary.ui.utils.InsetsHandler
import org.kaorun.diary.viewmodel.MainViewModel
import kotlin.math.abs

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

		auth = FirebaseAuth.getInstance()
		databaseReference = FirebaseDatabase.getInstance().getReference("Notes")

		if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
			navigateToWelcomeFragment()
		} else {
			showMainContent()
		}

		searchHistoryManager = SearchHistoryManager(this, "notes")

		setupInsets()
		setupRecyclerView()
		setupScrollBehavior()
		setupSearchManager()

		observeViewModel()

		val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
		itemTouchHelper.attachToRecyclerView(binding.recyclerView)

		binding.extendedFab.setOnClickListener {
			val intent = Intent(this, NoteActivity::class.java)
			startActivity(intent)
		}

		binding.chipSwitch.setOnClickListener {
			val intent = Intent(this, TasksMainActivity::class.java)
			val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in,
				android.R.anim.fade_out)
			startActivity(intent, options.toBundle())
			finish()
		}
	}

	private fun setupRecyclerView() {
		notesAdapter = NotesAdapter(
			notesList,
			onItemClicked = { noteId, noteTitle, noteContent ->
				if (actionMode == null) {
					// Open note if not in selection mode
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

	private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0,
		ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			return false
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			val position = viewHolder.adapterPosition

			val itemViewWidth = viewHolder.itemView.width
			val swipeDistance = abs(viewHolder.itemView.translationX)
			val noteId = notesAdapter.getNoteIdAtPosition(position)

			if (swipeDistance > itemViewWidth) {
				notesAdapter.removeItem(position)
				viewModel.deleteNotes(listOf(noteId))
			}
			else {
				notesAdapter.notifyItemChanged(position)
			}
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
			val itemView = viewHolder.itemView
			val backgroundColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError)
			val deleteIcon: Drawable = AppCompatResources.getDrawable(applicationContext, R.drawable.delete_24px)!!

			// Set up the Paint object
			val backgroundPaint = Paint().apply {
				color = backgroundColor
				isAntiAlias = true
				style = Paint.Style.FILL
			}

			val left = itemView.left.toFloat()
			val right = itemView.right.toFloat()
			val top = itemView.top.toFloat()
			val bottom = itemView.bottom.toFloat()

			val rectF = RectF(left, top, right, bottom)

			c.drawRoundRect(rectF, 32f, 32f, backgroundPaint)

			val iconColor = MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorOnError)
			DrawableCompat.setTint(deleteIcon, iconColor)

			val iconMargin = 32
			val iconTop = (itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2)
			val iconBottom = iconTop + deleteIcon.intrinsicHeight
			val iconLeft = if (dX > 0) { itemView.left + iconMargin } else { itemView.right - iconMargin - deleteIcon.intrinsicWidth }
			val iconRight = iconLeft + deleteIcon.intrinsicWidth

			deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
			deleteIcon.draw(c)

			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
		}
	}

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
			notesList = notesList,
			backPressedCallback = backPressedCallback,
		)

		binding.searchBar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.layoutSwitcher -> {
					switchLayout()
					if (isGridLayout) it.setIcon(R.drawable.view_agenda_24px)
					else it.setIcon(R.drawable.grid_view_24px)
				}
				R.id.signOut -> {
					FirebaseAuth.getInstance().signOut()
					navigateToWelcomeFragment()
				}
			}
			true
		}
	}

	private fun observeViewModel() {
		viewModel.isLoading.observe(this) {
				isLoading -> binding.progressBar.isVisible = isLoading
		}
		viewModel.notesList.observe(
			this
		) { notes ->
			notesList.clear()
			notesList.addAll(notes)
			notesAdapter.updateNotes(notes.toMutableList())
			binding.notesEmpty.notesEmptyLayout.isVisible = notes.isEmpty()

			val menuItem = binding.searchBar.menu.findItem(R.id.layoutSwitcher)
			menuItem?.isVisible = notes.isNotEmpty()
		}
	}

	private fun navigateToWelcomeFragment() {
		binding.recyclerView.visibility = View.GONE
		binding.searchBar.visibility = View.GONE
		binding.extendedFab.visibility = View.GONE
		binding.fragmentContainerView.visibility = View.VISIBLE

		// Create the WelcomeFragment instance
		val welcomeFragment = WelcomeFragment()

		// Begin the fragment transaction
		supportFragmentManager.beginTransaction()
			.replace(R.id.fragmentContainerView, welcomeFragment)
			.commit()
	}

	private fun showMainContent() {
		binding.recyclerView.visibility = View.VISIBLE
		binding.searchBar.visibility = View.VISIBLE
		binding.extendedFab.visibility = View.VISIBLE
		binding.fragmentContainerView.visibility = View.GONE
	}

	private fun setupInsets() {
		InsetsHandler.applyViewInsets(binding.recyclerView)
		InsetsHandler.applyFabInsets(binding.extendedFab)
		InsetsHandler.applyAppBarInsets(binding.appBarLayout)
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

