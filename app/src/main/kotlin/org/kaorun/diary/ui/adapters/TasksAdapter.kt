package org.kaorun.diary.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ItemTaskBinding

class TasksAdapter(
    private var tasks: List<TasksDatabase>,
    private val onItemClicked: (taskId: String, title: String, isCompleted: Boolean, time: String?) -> Unit,
    private val updateTask: (task: TasksDatabase) -> Unit // Add a callback to update task
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    // ViewHolder to hold each item view
    inner class TaskViewHolder(private val binding: ItemTaskBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TasksDatabase) {
            binding.taskTitle.text = task.title
            binding.date.text = task.time
            binding.date.isVisible = !task.time.isNullOrEmpty()

            binding.root.setOnClickListener {
                onItemClicked(task.id, task.title, task.isCompleted, task.time)
            }

            binding.checkbox.isChecked = task.isCompleted

            binding.checkbox.setOnClickListener {
                task.isCompleted = binding.checkbox.isChecked
                updateTask(task) // Update the task in Firebase when checkbox state changes
            }
        }
    }

    // Create a new view holder for each task item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    // Bind data to the views in the ViewHolder
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    // Return the number of tasks in the list
    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<TasksDatabase>) {
        tasks = newTasks // Update the data
        notifyDataSetChanged() // Refresh the RecyclerView
    }
}
