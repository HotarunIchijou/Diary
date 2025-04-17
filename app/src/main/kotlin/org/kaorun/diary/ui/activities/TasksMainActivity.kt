package org.kaorun.diary.ui.activities

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class TasksMainActivity : AppCompatActivity() {

    private lateinit var taskAdapter: TasksAdapter
    private lateinit var binding: ActivityTasksMainBinding
    private lateinit var searchTasksManager: SearchTasksManager
    private val taskList = mutableListOf<TasksDatabase>()
    private val tasksViewModel: TasksViewModel by viewModels()

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
            updateTask = { task -> tasksViewModel.updateTask(task) } // Pass the updateTask method
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

        binding.extendedFab.setOnClickListener {
            val bottomSheet = BottomSheetFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        binding.searchBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.signOut -> {
                    FirebaseAuth.getInstance().signOut()
                    navigateToWelcomeFragment()
                }
            }
            true
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        tasksViewModel.tasksList.observe(this) { tasks ->
            taskList.clear()
            taskList.addAll(tasks)
            taskAdapter.notifyDataSetChanged()
        }

        tasksViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
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


    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyFabInsets(binding.extendedFab)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }
}
