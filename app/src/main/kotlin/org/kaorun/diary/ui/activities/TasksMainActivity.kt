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
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ActivityTasksMainBinding
import org.kaorun.diary.ui.adapters.TasksAdapter
import org.kaorun.diary.ui.fragments.BottomSheetFragment
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

        createNotificationChannel(this)

        // Initialize RecyclerView and Adapter
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TasksAdapter(
            taskList,
            onItemClicked = { taskId, title, isCompleted, time ->
                val bottomSheet = BottomSheetFragment.newInstance(taskId, title, isCompleted, time)
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

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "reminder_channel",
            "Task reminders",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }


    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyFabInsets(binding.extendedFab)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }
}
