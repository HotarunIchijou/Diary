package org.kaorun.diary.ui.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ItemTaskBinding
import org.kaorun.diary.utils.DateUtils.formatDate

class TasksAdapter(
    private var tasks: List<TasksDatabase>,
    private val onItemClicked: (taskId: String, title: String, isCompleted: Boolean, time: String?, date: String?) -> Unit,
    private val onTaskChecked: (task: TasksDatabase, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    // ViewHolder to hold each item view
    inner class TaskViewHolder(private val binding: ItemTaskBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TasksDatabase) {
            val formattedDate = when {
                task.date.isNullOrEmpty() && task.time.isNullOrEmpty() -> null
                task.date.isNullOrEmpty() -> task.time
                task.time.isNullOrEmpty() -> formatDate(binding.root.context, task.date)
                else -> {
                    val formattedDate = formatDate(binding.root.context, task.date)
                    if (formattedDate != null) "${formattedDate}, ${task.time}" else task.time
                }
            }


            binding.taskTitle.text = task.title
            binding.date.text = formattedDate
            val color = TypedValue()
            binding.root.context.theme.resolveAttribute(
                if (task.isCompleted) com.google.android.material.R.attr.colorOnSurfaceVariant
                else android.R.attr.colorPrimary,
                color,
                true
            )
            binding.date.setTextColor(color.data)



            binding.date.isVisible = !formattedDate.isNullOrEmpty()
            binding.root.setOnClickListener {
                onItemClicked(task.id, task.title, task.isCompleted, task.time, task.date)
            }

            binding.checkbox.isChecked = task.isCompleted

            binding.checkbox.setOnClickListener {
                val updatedTask = task.copy(isCompleted = binding.checkbox.isChecked)
                onTaskChecked(updatedTask, binding.checkbox.isChecked)
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
