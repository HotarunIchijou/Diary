package org.kaorun.diary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.kaorun.diary.data.TasksDatabase

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
}
