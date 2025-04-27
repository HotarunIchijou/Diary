package org.kaorun.diary.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat.getString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.receivers.NotificationReceiver
import org.kaorun.diary.utils.TasksLocalCache
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksViewModel : ViewModel() {

	private val _tasksList = MutableLiveData<List<TasksDatabase>>()
	val tasksList: LiveData<List<TasksDatabase>> get() = _tasksList

	private val firebaseAuth = FirebaseAuth.getInstance()
	private val databaseReference: DatabaseReference =
		FirebaseDatabase.getInstance().getReference("Tasks")
	private val tasks = mutableListOf<TasksDatabase>()
	private val _isLoading = MutableLiveData<Boolean>()
	val isLoading: LiveData<Boolean> get() = _isLoading


	init {
		fetchTasks()
	}

	private fun fetchTasks() {
		_isLoading.value = true
		val userId = firebaseAuth.currentUser?.uid ?: return

		attachChildEventListener(userId) // Attach listener

		// Initial check for tasks existence
		databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (!snapshot.exists()) {
					// If no tasks exist, update the state
					_tasksList.value = emptyList()
					_isLoading.value = false
				}
			}

			override fun onCancelled(error: DatabaseError) {
				_isLoading.value = false
			}
		})
	}

	private fun attachChildEventListener(userId: String) {
		databaseReference.child(userId).addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				val value = snapshot.value

				when (value) {
					is Map<*, *> -> {
						val task = snapshot.getValue(TasksDatabase::class.java)
						if (task != null) {
							tasks.add(task)
							_tasksList.value = tasks.toList()
						}
						_isLoading.value = false
					}

					else -> {
						_isLoading.value = false
					}
				}
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				val updatedTask = snapshot.getValue(TasksDatabase::class.java)
				if (updatedTask != null) {
					val index = tasks.indexOfFirst { it.id == updatedTask.id }
					if (index != -1) {
						tasks[index] = updatedTask
						_tasksList.value = tasks.toList()
					}
				}
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
				val taskId = snapshot.key.orEmpty()
				val index = tasks.indexOfFirst { it.id == taskId }
				if (index != -1) {
					tasks.removeAt(index)
					_tasksList.value = tasks.toList()
				}
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
			override fun onCancelled(error: DatabaseError) {
				_isLoading.value = false
			}
		})
	}

	fun addTask(task: TasksDatabase) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		val taskRef = databaseReference.child(userId).child(task.id)

		taskRef.setValue(task).addOnCompleteListener { taskResult ->
			if (taskResult.isSuccessful) _isLoading.value = false
		}
	}

	fun updateTask(task: TasksDatabase) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		val taskRef = databaseReference.child(userId).child(task.id)

		taskRef.setValue(task).addOnCompleteListener { taskResult ->
			if (taskResult.isSuccessful) {
				val index = tasks.indexOfFirst { it.id == task.id }
				if (index != -1) {
					tasks[index] = task
					_tasksList.value = tasks.toList() // Notify observers that the task list has been updated
				}
			}
		}
	}

	fun deleteTask(taskId: String) {
		val userId = firebaseAuth.currentUser?.uid ?: return

			databaseReference.child(userId).child(taskId).removeValue()
			val index = tasks.indexOfFirst { it.id == taskId }
			tasks.removeAt(index)
			_tasksList.value = tasks.toList()
	}

	companion object {
		fun restoreNotifications(context: Context) {
			val tasks = TasksLocalCache.getCachedTasks(context)

			for (task in tasks) {
				if (!task.isCompleted && task.date != null && task.time != null) {
					val taskDate = parseDateAndTime(task.date, task.time)

					if (taskDate != null && taskDate.time > System.currentTimeMillis()) {
						scheduleNotification(context, taskDate, task.id, task.title)
					}
				}
			}
		}

		private fun parseDateAndTime(dateStr: String, timeStr: String): Date? {
			return try {
				val formatter = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
				val combined = "$dateStr $timeStr"
				formatter.parse(combined)
			} catch (_: Exception) {
				null
			}
		}

		private fun scheduleNotification(context: Context, date: Date, taskId: String, taskTitle: String) {
			val intent = Intent(context, NotificationReceiver::class.java).apply {
				putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
				putExtra(NotificationReceiver.EXTRA_TITLE, taskTitle)
			}

			val pendingIntent = android.app.PendingIntent.getBroadcast(
				context,
				taskId.hashCode(),
				intent,
				android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
			)

			try {
				val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
				alarmManager.setExactAndAllowWhileIdle(
					android.app.AlarmManager.RTC_WAKEUP,
					date.time,
					pendingIntent
				)

            } catch (_: SecurityException) {
				Toast.makeText(context, getString(context, R.string.grant_reminders_permission), Toast.LENGTH_SHORT).show()
				context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
		}
	}
}
