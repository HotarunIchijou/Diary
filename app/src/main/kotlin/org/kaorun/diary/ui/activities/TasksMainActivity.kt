package org.kaorun.diary.ui.activities

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ActivityTasksMainBinding
import org.kaorun.diary.ui.adapters.TasksAdapter
import org.kaorun.diary.ui.fragments.BottomSheetFragment
import org.kaorun.diary.ui.fragments.WelcomeFragment
import org.kaorun.diary.ui.managers.SearchTasksManager
import org.kaorun.diary.ui.utils.InsetsHandler
import org.kaorun.diary.viewmodel.TasksViewModel

class TasksMainActivity : BaseActivity() {

    private lateinit var taskAdapter: TasksAdapter
    private lateinit var binding: ActivityTasksMainBinding
    private lateinit var searchTasksManager: SearchTasksManager
    private val taskList = mutableListOf<TasksDatabase>()
    private val tasksViewModel: TasksViewModel by viewModels()
    private var selected = 0
    private var taskIdFromNotification: String? = null
    private var titleFromNotification: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set the layout for the activity
        setupInsets()
        setupScrollBehavior()

        createNotificationChannel(this)

        // Initialize RecyclerView and Adapter
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TasksAdapter(
            taskList,
            onItemClicked = { taskId, title, isCompleted, time, date ->
                val bottomSheet = BottomSheetFragment.newInstance(taskId, title, isCompleted, time, date)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            },
            // Pass the updateTask method
            onTaskChecked = { task, isChecked ->
                if (isChecked) {
                    showUndoSnackbar(task)
                }
                tasksViewModel.updateTask(task)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        searchTasksManager = SearchTasksManager(
            binding = binding,
            onBackPressedDispatcher = onBackPressedDispatcher,
            tasksAdapter = taskAdapter,
            lifecycleOwner = this,
            tasksList = taskList
        )

        binding.chipSwitch.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in,
                android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
            finish()
        }

        binding.chipTasksFilter.setOnClickListener {
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


        binding.extendedFab.setOnClickListener {
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
        binding.extendedFab.visibility = View.GONE
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

        binding.tasksEmpty.tasksEmptyLayout.isVisible = tasks.isEmpty()

        binding.chipTasksFilter.apply {
            when (selected) {
                0 -> {
                    text = getString(R.string.pending)
                    chipIcon = AppCompatResources.getDrawable(context, R.drawable.pending_actions_24px)
                }
                1 -> {
                    text = getString(R.string.completed)
                    chipIcon = AppCompatResources.getDrawable(context, R.drawable.inventory_24px)
                }
            }
        }
    }

    private fun showUndoSnackbar(task: TasksDatabase) {
        Snackbar.make(binding.root, getString(R.string.task_completed), Snackbar.LENGTH_LONG)
            .setAnchorView(binding.extendedFab)
            .setAction(
                getString(R.string.undo)
            ) {
            val undoneTask = task.copy(isCompleted = false)
            tasksViewModel.updateTask(undoneTask)
        }.show()
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyFabInsets(binding.extendedFab)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }
}
