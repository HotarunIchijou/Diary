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

	init {
		fetchNotes()
	}

	private fun fetchNotes() {
		val userId = firebaseAuth.currentUser?.uid ?: return
		databaseReference.child(userId).addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
				val noteId = snapshot.key.orEmpty()
				val noteTitle = snapshot.getValue(String::class.java) ?: ""
				val note = NotesDatabase(id = noteId, title = noteTitle)
				notes.add(note)
				_notesList.value = notes.toList()
			}

			override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
				val noteId = snapshot.key.orEmpty()
				val noteTitle = snapshot.getValue(String::class.java) ?: ""
				val index = notes.indexOfFirst { it.id == noteId }
				if (index != -1) {
					notes[index] = NotesDatabase(id = noteId, title = noteTitle)
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
			override fun onCancelled(error: DatabaseError) {}
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
