package org.kaorun.diary.ui.adapters

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemViewHolder
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.ItemTaskBinding
import org.kaorun.diary.utils.DateUtils.formatDate

class TasksAdapter(
    private var tasks: List<TasksDatabase>,
    private val onItemClicked: (
        taskId: String,
        title: String,
        isCompleted: Boolean,
        time: String?,
        date: String?
    ) -> Unit,
    private val onTaskChecked: (task: TasksDatabase, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<ListItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListItemViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(position, itemCount)

        val binding = ItemTaskBinding.bind(holder.itemView)
        val task = tasks[position]

        val formattedDate = when {
            task.date.isNullOrEmpty() && task.time.isNullOrEmpty() -> null
            task.date.isNullOrEmpty() -> task.time
            task.time.isNullOrEmpty() -> formatDate(binding.root.context, task.date)
            else -> {
                val date = formatDate(binding.root.context, task.date)
                if (date != null) "$date, ${task.time}" else task.time
            }
        }

        binding.taskTitle.text = task.title
        binding.date.text = formattedDate
        binding.date.isVisible = !formattedDate.isNullOrEmpty()

        val color = TypedValue()
        binding.root.context.theme.resolveAttribute(
            if (task.isCompleted)
                com.google.android.material.R.attr.colorOnSurfaceVariant
            else
                android.R.attr.colorPrimary,
            color,
            true
        )
        binding.date.setTextColor(color.data)

        binding.checkbox.isChecked = task.isCompleted

        binding.root.setOnClickListener {
            onItemClicked(
                task.id,
                task.title,
                task.isCompleted,
                task.time,
                task.date
            )
        }

        binding.checkbox.setOnClickListener {
            onTaskChecked(
                task.copy(isCompleted = binding.checkbox.isChecked),
                binding.checkbox.isChecked
            )
        }
    }

    override fun getItemCount(): Int = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<TasksDatabase>) {
        tasks = newTasks.asReversed()
        notifyDataSetChanged()
    }
}
