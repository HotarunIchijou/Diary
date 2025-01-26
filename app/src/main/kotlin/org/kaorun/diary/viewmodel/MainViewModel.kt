package org.kaorun.diary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.kaorun.diary.data.NotesDatabase

class MainViewModel : ViewModel() {

	private val _notesList = MutableLiveData<List<NotesDatabase>>()
	val notesList: LiveData<List<NotesDatabase>> get() = _notesList

	private val firebaseAuth = FirebaseAuth.getInstance()
	private val databaseReference: DatabaseReference =
		FirebaseDatabase.getInstance().getReference("Notes")
	private val notes = mutableListOf<NotesDatabase>()
	private val _isLoading = MutableLiveData<Boolean>()
	val isLoading: LiveData<Boolean> get() = _isLoading


	init {
		fetchNotes()
	}

	private fun fetchNotes() {
		_isLoading.value = true
		val userId = firebaseAuth.currentUser?.uid ?: return

		attachChildEventListener(userId) // Attach listener

		// Initial check for notes existence
		databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (!snapshot.exists()) {
					// If no notes exist, update the state
					_notesList.value = emptyList()
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
				val noteId = snapshot.key.orEmpty()
				val value = snapshot.value

				when (value) {
					is String -> {
						// Handling the old structure: "id:title"
						val noteTitle = snapshot.getValue(String::class.java) ?: ""  // Get title from old structure
						val noteContent = ""  // Old structure doesn't have content, so it's left empty
						val note = NotesDatabase(id = noteId, title = noteTitle, note = noteContent)
						notes.add(note)
						_notesList.value = notes.toList()
						_isLoading.value = false
					}

					is Map<*, *> -> {
						val noteData = snapshot.getValue(object : GenericTypeIndicator<Map<String, String>>() {})
						val noteTitle = noteData?.get("title") ?: ""
						val noteContent = noteData?.get("note") ?: ""
						val note = NotesDatabase(id = noteId, title = noteTitle, note = noteContent)
						notes.add(note)
						_notesList.value = notes.toList()
						_isLoading.value = false
					}

					else -> {
						_isLoading.value = false
					}
				}
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				val noteId = snapshot.key.orEmpty()
				val noteData = snapshot.getValue(object : GenericTypeIndicator<Map<String, String>>() {})

				val noteTitle = noteData?.get("title") ?: ""
				val noteContent = noteData?.get("note") ?: ""
				val index = notes.indexOfFirst { it.id == noteId }
				if (index != -1) {
					notes[index] = NotesDatabase(id = noteId, title = noteTitle, note = noteContent)
					_notesList.value = notes.toList()
				}
			}

			override fun onChildRemoved(snapshot: DataSnapshot) {
				val noteId = snapshot.key.orEmpty()
				val index = notes.indexOfFirst { it.id == noteId }
				if (index != -1) {
					notes.removeAt(index)
					_notesList.value = notes.toList()
				}
			}

			override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
			override fun onCancelled(error: DatabaseError) {
				_isLoading.value = false
			}
		})
	}

	fun deleteNotes(noteIds: List<String>) {
		val userId = firebaseAuth.currentUser?.uid ?: return
		for (noteId in noteIds) {
			databaseReference.child(userId).child(noteId).removeValue()
			val index = notes.indexOfFirst { it.id == noteId }
			if (index != -1) {
				notes.removeAt(index)
				_notesList.value = notes.toList()
			}
		}
	}
}
