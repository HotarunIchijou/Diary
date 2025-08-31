package org.kaorun.diary.ui.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.sidesheet.SideSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ActivityTasksMainBinding
import org.kaorun.diary.ui.activities.settings.SettingsActivity
import org.kaorun.diary.ui.adapters.TasksAdapter
import org.kaorun.diary.ui.fragments.BottomSheetFragment
import org.kaorun.diary.ui.fragments.WelcomeFragment
import org.kaorun.diary.ui.managers.SearchTasksManager
import org.kaorun.diary.utils.InsetsHandler
import org.kaorun.diary.utils.NotificationUtils.cancelNotification
import org.kaorun.diary.utils.VerticalSpaceItemDecoration
import org.kaorun.diary.viewmodel.TasksViewModel

class TasksMainActivity : BaseActivity() {

    private lateinit var taskAdapter: TasksAdapter
    private lateinit var binding: ActivityTasksMainBinding
    private lateinit var searchTasksManager: SearchTasksManager
    private val taskList = mutableListOf<TasksDatabase>()
    private val tasksViewModel: TasksViewModel by viewModels()
    private var backPressedCallback: OnBackPressedCallback? = null
    private var selected = 0
    private var taskIdFromNotification: String? = null
    private var titleFromNotification: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupRecyclerView()
        setupScrollBehavior()
        setupSideSheetButton()

        createNotificationChannel(this)

        binding.switchFilerButton.setOnClickListener {
            val options = arrayOf(getString(R.string.pending), getString(R.string.completed))

            MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setIcon(R.drawable.filter_alt_24px)
                .setTitle(getString(R.string.tasks_filter_title))
                .setSingleChoiceItems(options, selected) { dialog, which ->
                    selected = which
                    applyFilter(which)
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        binding.fab.setOnClickListener {
            val bottomSheet = BottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.signOut -> {
                    FirebaseAuth.getInstance().signOut()
                    navigateToWelcomeFragment()
                }
            }
            true
        }

        taskIdFromNotification = intent.getStringExtra("task_id")
        titleFromNotification = intent.getStringExtra("notification_title")

        observeViewModel()
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TasksAdapter(
            taskList,
            onItemClicked = { taskId, title, isCompleted, time, date ->
                val bottomSheet =
                    BottomSheetFragment.newInstance(taskId, title, isCompleted, time, date)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            },
            onTaskChecked = { task, isChecked ->
                if (isChecked) {
                    showUndoSnackbar(task)
                    cancelNotification(this, task.id)
                }
                tasksViewModel.updateTask(this, task)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(this))

        searchTasksManager = SearchTasksManager(
            binding = binding,
            onBackPressedDispatcher = onBackPressedDispatcher,
            tasksAdapter = taskAdapter,
            lifecycleOwner = this,
            tasksList = taskList,
            backPressedCallback = backPressedCallback
        ) {
            setupSideSheetButton()
        }
    }

    private fun observeViewModel() {
        tasksViewModel.tasksList.observe(this) { tasks ->
            taskList.clear()
            taskList.addAll(tasks)
            applyFilter(selected)

            if (taskIdFromNotification != null && titleFromNotification != null) {
                val task = tasks.find { it.id == taskIdFromNotification }
                if (task != null) {
                    val bottomSheet = BottomSheetFragment.newInstance(
                        task.id,
                        task.title,
                        task.isCompleted,
                        task.time,
                        task.date
                    )
                    bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                    taskIdFromNotification = null
                    titleFromNotification = null
                }
            }
        }

        tasksViewModel.isLoading.observe(this) { isLoading ->
            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }


    private fun setupScrollBehavior() {
        val fab = binding.fab
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 12 && fab.isVisible) fab.hide()
                if (dy < -12 && !fab.isVisible) fab.show()
            }
        })
    }

    private fun setupSideSheetButton() {
        binding.sideSheetButton.icon = AppCompatResources.getDrawable(
            binding.tasksMainActivity.context,
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
            val tasks = navigationView.menu.findItem(R.id.tasks)
            tasks?.isChecked = true

            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.notes -> {
                        finish()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        sideSheetDialog.hide()
                    }

                    R.id.tasks -> {
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

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "reminder_channel",
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
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

    private fun applyFilter(filterType: Int) {
        val filtered = when (filterType) {
            0 -> taskList.filter { !it.isCompleted } // Pending
            1 -> taskList.filter { it.isCompleted }  // Completed
            else -> taskList
        }
        updateList(filtered)
    }

    private fun updateList(tasks: List<TasksDatabase>) {
        taskAdapter.updateTasks(tasks)

        when {
            tasks.isEmpty() -> {
                binding.tasksEmpty.tasksEmptyLayout.isVisible = true
                binding.nothingFoundTasks.nothingFoundTasksLayout.isVisible = false
            }
            taskAdapter.itemCount == 0 -> {
                binding.tasksEmpty.tasksEmptyLayout.isVisible = false
                binding.nothingFoundTasks.nothingFoundTasksLayout.isVisible = true
            }
            else -> {
                binding.tasksEmpty.tasksEmptyLayout.isVisible = false
                binding.nothingFoundTasks.nothingFoundTasksLayout.isVisible = false
            }
        }
    }

    private fun showUndoSnackbar(task: TasksDatabase) {
        Snackbar.make(binding.root, getString(R.string.task_completed), Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fab)
            .setAction(
                getString(R.string.undo)
            ) {
            val undoneTask = task.copy(isCompleted = false)
            tasksViewModel.updateTask(this, undoneTask)
        }.show()
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyFabInsets(binding.fab)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }
}
