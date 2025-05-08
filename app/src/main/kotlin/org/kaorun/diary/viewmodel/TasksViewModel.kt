package org.kaorun.diary.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import java.time.LocalDate
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
		attachChildEventListener(userId)
		databaseReference.child(userId)
			.addListenerForSingleValueEvent(object : ValueEventListener {
				override fun onDataChange(snapshot: DataSnapshot) {
					if (!snapshot.exists()) {
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
		databaseReference.child(userId)
			.addChildEventListener(object : ChildEventListener {
				override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
					val data = snapshot.value as? Map<*, *> ?: run {
						_isLoading.value = false
						return
					}
					val task = mapToTask(snapshot.key.orEmpty(), data)
					tasks.add(task)
					_tasksList.value = tasks.toList()
					_isLoading.value = false
				}

				override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
					val data = snapshot.value as? Map<*, *> ?: return
					val updated = mapToTask(snapshot.key.orEmpty(), data)
					val idx = tasks.indexOfFirst { it.id == updated.id }
					if (idx != -1) {
						tasks[idx] = updated
						_tasksList.value = tasks.toList()
					}
				}

				override fun onChildRemoved(snapshot: DataSnapshot) {
					val removedId = snapshot.key.orEmpty()
					val idx = tasks.indexOfFirst { it.id == removedId }
					if (idx != -1) {
						tasks.removeAt(idx)
						_tasksList.value = tasks.toList()
					}
				}

				override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
				override fun onCancelled(error: DatabaseError) {
					_isLoading.value = false
				}
			})
	}

	private fun mapToTask(key: String, data: Map<*, *>): TasksDatabase {
		return TasksDatabase(
			id          = key,
			title       = data["title"]       as? String  ?: "",
			isCompleted = data["isCompleted"] as? Boolean ?: false,
			time        = data["time"]        as? String,
			date        = data["date"]        as? String
		)
	}

	fun addTask(context: Context, task: TasksDatabase) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		databaseReference.child(userId).child(task.id)
			.setValue(task.toMap())
			.addOnCompleteListener { result ->
				_isLoading.value = false
				if (result.isSuccessful) updateLocalCache(context)
			}
	}

	fun updateTask(context: Context, task: TasksDatabase) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		databaseReference.child(userId).child(task.id)
			.setValue(task.toMap())
			.addOnCompleteListener { result ->
				if (result.isSuccessful) {
					val idx = tasks.indexOfFirst { it.id == task.id }
					if (idx != -1) {
						tasks[idx] = task
						_tasksList.value = tasks.toList()
						updateLocalCache(context)
					}
				}
			}
	}

	fun deleteTask(context: Context, taskId: String) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		databaseReference.child(userId).child(taskId).removeValue()
		val idx = tasks.indexOfFirst { it.id == taskId }
		if (idx != -1) {
			tasks.removeAt(idx)
			_tasksList.value = tasks.toList()
			updateLocalCache(context)
		}
	}

	private fun updateLocalCache(context: Context) {
		TasksLocalCache.cacheTasks(context, tasks)
	}

	private val bottomSheetDismissed = MutableLiveData<Boolean>()

	fun setBottomSheetDismissed(dismissed: Boolean) {
		bottomSheetDismissed.value = dismissed
	}

	fun getBottomSheetDismissed(): LiveData<Boolean> {
		return bottomSheetDismissed
	}

	companion object {
		fun restoreNotifications(context: Context) {
			val cached = TasksLocalCache.getCachedTasks(context)
			for (task in cached) {
				if (!task.isCompleted && task.date != null && task.time != null) {
					val dateObj = parseDateAndTime(task.date, task.time)
					if (dateObj != null && dateObj.time > System.currentTimeMillis()) {
						scheduleNotification(context, dateObj, task.id, task.title)
					}
				}
			}
		}

		private fun parseDateAndTime(date: String, time: String): Date? {
			return try {
				val dateWithYear = if (date.contains(",")) date
				else "${date.trim()}, ${LocalDate.now().year}"
				val combined = "$dateWithYear $time"
				SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).parse(combined)
			} catch (_: Exception) {
				null
			}
		}

		private fun scheduleNotification(
			context: Context,
			date: Date,
			taskId: String,
			taskTitle: String
		) {
			val intent = Intent(context, NotificationReceiver::class.java).apply {
				putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
				putExtra(NotificationReceiver.EXTRA_TITLE, taskTitle)
			}
			val pi = PendingIntent.getBroadcast(
				context,
				taskId.hashCode(),
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
			)
			try {
				val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
				am.setExactAndAllowWhileIdle(
					AlarmManager.RTC_WAKEUP,
					date.time,
					pi
				)
			} catch (_: SecurityException) {
				Toast.makeText(
					context,
					context.getString(R.string.grant_reminders_permission),
					Toast.LENGTH_SHORT
				).show()
				context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
			}
		}
	}
}

