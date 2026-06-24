package org.kaorun.diary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NoteViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance()
            .reference.child("Notes")
            .child(auth.currentUser!!.uid)
    }

    var noteId: String? = null
        private set

    private var lastSavedTitle: String? = null
    private var lastSavedContent: String? = null
    private var isNoteDeleted = false

    private val _showSaveButton = MutableLiveData(false)
    val showSaveButton: LiveData<Boolean> = _showSaveButton

    private val _closeScreen = MutableLiveData(false)
    val closeScreen: LiveData<Boolean> = _closeScreen

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _noteLoaded = MutableLiveData<Pair<String, String>>()
    val noteLoaded: LiveData<Pair<String, String>> = _noteLoaded


    fun setNoteId(id: String?) {
        noteId = id
        id?.let { loadNote(it) }
    }

    fun onTextChanged(title: String, content: String) {
        val hasChanges =
            title != lastSavedTitle || content != lastSavedContent

        val noteNotEmpty =
            !title.isRichTextEmpty() || !content.isRichTextEmpty()

        _showSaveButton.value = hasChanges && noteNotEmpty
    }

    fun saveNote(title: String, content: String, closeAfter: Boolean = false) {
        if (title.isRichTextEmpty() && content.isRichTextEmpty()) return

        val noteData = mapOf(
            "title" to title.trim(),
            "note" to content.trim()
        )

        if (noteId != null) {
            databaseRef.child(noteId!!).setValue(noteData)
        } else {
            databaseRef.push().also {
                noteId = it.key
                it.setValue(noteData)
            }
        }

        lastSavedTitle = title
        lastSavedContent = content
        _showSaveButton.value = false

        if (closeAfter) {
            _closeScreen.value = true
        }
    }

    fun deleteNote() {
        if (noteId == null) {
            isNoteDeleted = true
            _closeScreen.value = true
            return
        }

        databaseRef.child(noteId!!).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isNoteDeleted = true
                    _closeScreen.value = true
                } else {
                    _errorMessage.value =
                        task.exception?.message ?: "Failed to delete note"
                }
            }
    }

    fun autoSave(title: String, content: String) {
        if (isNoteDeleted) return

        if (title != lastSavedTitle || content != lastSavedContent) {
            saveNote(title, content)
        }
    }


    private fun loadNote(id: String) {
        databaseRef.child(id).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) return@addOnSuccessListener

            val title = snapshot.child("title")
                .getValue(String::class.java).orEmpty()
            val content = snapshot.child("note")
                .getValue(String::class.java).orEmpty()

            lastSavedTitle = title
            lastSavedContent = content

            _noteLoaded.value = title to content
            _showSaveButton.value = false
        }
    }

    private fun String.isRichTextEmpty(): Boolean {
        return this
            .replace(Regex("<br\\s*/?>"), "")
            .replace(Regex("<p>|</p>"), "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
            .isEmpty()
    }
}
