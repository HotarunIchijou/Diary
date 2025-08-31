package org.kaorun.diary.ui.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.sidesheet.SideSheetDialog
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
import org.kaorun.diary.utils.InsetsHandler
import org.kaorun.diary.viewmodel.MainViewModel
import kotlin.math.abs

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
	private var isGridLayout = false

    override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		applySavedTheme()
		checkNotificationPermission()

		auth = FirebaseAuth.getInstance()
		databaseReference = FirebaseDatabase.getInstance().getReference("Notes")

		if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
			navigateToWelcomeFragment()
		} else {
			addShortcuts()
			showMainContent()
		}

		searchHistoryManager = SearchHistoryManager(this, "notes")

		setupInsets()
		setupRecyclerView()
		setupScrollBehavior()
		setupSearchManager()
		setupSideSheetButton()
		setupSwitchLayoutButton()
		setupContextualToolbar()

		observeViewModel()

		val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
		itemTouchHelper.attachToRecyclerView(binding.recyclerView)

		binding.fab.setOnClickListener {
			val intent = Intent(this, NoteActivity::class.java)
			startActivity(intent)
		}
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
			val position = viewHolder.absoluteAdapterPosition

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
			val backgroundColor = MaterialColors.getColor(itemView, androidx.appcompat.R.attr.colorError)
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

			c.drawRoundRect(rectF, 56f, 56f, backgroundPaint)

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
				if (isGridLayout) this.setIconResource(R.drawable.view_agenda_24px)
				else this.setIconResource(R.drawable.grid_view_24px)
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

	private fun setupInsets() {
		InsetsHandler.applyViewInsets(binding.recyclerView)
		InsetsHandler.applyFabInsets(binding.fab)
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

	private fun applySavedTheme() {
		val themeMode = PreferenceManager.getDefaultSharedPreferences(this)
			.getInt("theme_mode", 0)

		val mode = when (themeMode) {
			1 -> AppCompatDelegate.MODE_NIGHT_NO
			2 -> AppCompatDelegate.MODE_NIGHT_YES
			else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		}

		AppCompatDelegate.setDefaultNightMode(mode)
	}

    private fun checkNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
		}
	}

	private fun setupContextualToolbar() {
		binding.contextualToolbar.apply {
			inflateMenu(R.menu.menu_select_appbar)
			setNavigationIcon(R.drawable.close_24px)
			setNavigationOnClickListener { hideContextualToolbarAndClearSelection() }

			setOnMenuItemClickListener { item ->
				when (item.itemId) {
					R.id.delete -> {
						viewModel.deleteNotes(notesAdapter.getSelectedNotes())
						hideContextualToolbarAndClearSelection()
						true
					}
					else -> false
				}
			}
		}


		InsetsHandler.applyAppBarInsets(binding.contextualToolbarContainer)
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
}

