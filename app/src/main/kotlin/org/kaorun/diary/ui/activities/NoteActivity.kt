package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.onegravity.rteditor.RTEditText
import com.onegravity.rteditor.RTManager
import com.onegravity.rteditor.api.RTApi
import com.onegravity.rteditor.api.RTMediaFactoryImpl
import com.onegravity.rteditor.api.RTProxyImpl
import com.onegravity.rteditor.api.format.RTFormat
import org.kaorun.diary.R
import org.kaorun.diary.databinding.ActivityNoteBinding
import org.kaorun.diary.utils.FloatingToolbarHelper
import org.kaorun.diary.utils.InsetsHandler
import androidx.core.view.isVisible

class NoteActivity : BaseActivity() {

	private lateinit var binding: ActivityNoteBinding
	private lateinit var databaseRef: DatabaseReference
	private lateinit var auth: FirebaseAuth
	private lateinit var rtManager: RTManager
	private lateinit var title: RTEditText
	private lateinit var note: RTEditText
	private var noteId: String? = null
	private var isNoteDeleted: Boolean = false
	private var lastSavedTitle: String? = null
	private var lastSavedContent: String? = null
	private var hasChanges: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityNoteBinding.inflate(layoutInflater)
		setContentView(binding.root)

		InsetsHandler.applyAppBarInsets(binding.appBarLayout)
		InsetsHandler.applyViewInsets(binding.noteTitle, ignoreBottomPadding = true)
		InsetsHandler.applyDividerInsets(binding.titleDivider)
		InsetsHandler.applyViewInsets(binding.noteContent)

		val rtApi = RTApi(this, RTProxyImpl(this), RTMediaFactoryImpl(this, true))
		rtManager = RTManager(rtApi, savedInstanceState)

		title = binding.noteTitle
		rtManager.registerEditor(title, true)

		note = binding.noteContent
		rtManager.registerEditor(note, true)

		val toolbarHelper = FloatingToolbarHelper(rtManager, binding)
		toolbarHelper.setupFloatingToolbar()

		val floatingToolbar = binding.floatingToolbar
		val am = applicationContext.getSystemService(AccessibilityManager::class.java)
		if (am != null && am.isTouchExplorationEnabled) {
			(floatingToolbar.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = null
			floatingToolbar.post {
				note.setPadding(
					note.paddingLeft,
					note.paddingTop,
					note.paddingRight,
					note.paddingBottom + floatingToolbar.measuredHeight
				)
			}
		}

		if (Intent.ACTION_SEND == intent.action && intent.type != null) {
			if ("text/plain" == intent.type) {
				val noteContent = intent.getStringExtra(Intent.EXTRA_TEXT)
				if (noteContent != null) note.setRichTextEditing(true, noteContent)
			}
		}
		else {
			noteId = intent.getStringExtra("NOTE_ID")
			noteId?.let { loadNote(it) }
		}

		binding.noteTitle.requestFocus()
		binding.buttonSave.visibility = View.GONE

		registerEvents()
	}

	private fun registerEvents() {
		binding.noteTitle.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) = checkForChanges()
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
				if (binding.noteTitle.lineCount > 5) {
					val currentText = binding.noteTitle.text?.toString() ?: ""
					binding.noteTitle.setText(currentText.substring(0, currentText.length - count))
					binding.noteTitle.text?.let { binding.noteTitle.setSelection(it.length) }
				}
			}
		})

		binding.noteContent.addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(s: Editable?) = checkForChanges()
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
		})

		binding.topAppBar.setNavigationOnClickListener { finish() }
		binding.topAppBar.setOnMenuItemClickListener { menuItem ->
			when (menuItem.itemId) {
				R.id.delete -> {
					deleteNote()
					true
				}
				else -> false
			}
		}

		binding.buttonSave.setOnClickListener {
			saveNote(noteId)
			lastSavedTitle = title.getText(RTFormat.HTML).trim()
			lastSavedContent = note.getText(RTFormat.HTML).trim()
			hasChanges = false
			hideSaveButton()
			finish()
		}
	}

	private fun checkForChanges() {
		val currentTitle = title.getText(RTFormat.HTML).trim()
		val currentContent = note.getText(RTFormat.HTML).trim()
		val changesDetected = currentTitle != lastSavedTitle || currentContent != lastSavedContent
		val noteNotEmpty = currentTitle.isNotEmpty() || currentContent.isNotEmpty()

		if (changesDetected && noteNotEmpty) showSaveButton()
		else hideSaveButton()
	}

	private fun showSaveButton() {
		if (binding.buttonSave.isVisible) return
		val fade = MaterialFade().apply { duration = 150L }
		TransitionManager.beginDelayedTransition(binding.root, fade)
		binding.buttonSave.isVisible = true
	}

	private fun hideSaveButton() {
		if (binding.buttonSave.visibility != View.VISIBLE) return
		val fade = MaterialFade().apply { duration = 84L }
		TransitionManager.beginDelayedTransition(binding.root, fade)
		binding.buttonSave.isVisible = false
	}

	private fun saveNote(noteId: String?) {
		val titleText = title.getText(RTFormat.HTML).trim()
		val noteText = note.getText(RTFormat.HTML).trim()

		auth = FirebaseAuth.getInstance()
		databaseRef = FirebaseDatabase.getInstance()
			.reference.child("Notes").child(auth.currentUser?.uid.toString())

		val noteData = mapOf("title" to titleText, "note" to noteText)

		if (noteId != null) {
			databaseRef.child(noteId).setValue(noteData).addOnCompleteListener {
				if (!it.isSuccessful) Toast.makeText(applicationContext,
					it.exception?.message,
					Toast.LENGTH_SHORT).show()
			}
		} else if (noteText.isNotEmpty() || titleText.isNotEmpty()) {
			databaseRef.push().setValue(noteData).addOnCompleteListener {
				if (!it.isSuccessful) Toast.makeText(applicationContext,
					it.exception?.message,
					Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun deleteNote() {
		if (noteId != null) {
			auth = FirebaseAuth.getInstance()
			databaseRef = FirebaseDatabase.getInstance()
				.reference.child("Notes").child(auth.currentUser?.uid.toString())

			databaseRef.child(noteId!!).removeValue().addOnCompleteListener { task ->
				if (task.isSuccessful) {
					isNoteDeleted = true
					finish()
				}
				else Toast.makeText(this, "Failed to delete note: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
			}
		}
		else {
			isNoteDeleted = true
			finish()
		}
	}

	private fun loadNote(noteId: String) {
		auth = FirebaseAuth.getInstance()
		databaseRef = FirebaseDatabase.getInstance()
			.reference.child("Notes").child(auth.currentUser?.uid.toString())
		databaseRef.child(noteId).get().addOnSuccessListener { snapshot ->
			if (snapshot.exists()) {
				val noteTitle = snapshot.child("title").getValue(String::class.java) ?: ""
				val noteContent = snapshot.child("note").getValue(String::class.java) ?: ""

				title.setRichTextEditing(true, noteTitle)
				note.setRichTextEditing(true, noteContent)

				lastSavedTitle = noteTitle
				lastSavedContent = noteContent
				hasChanges = false
				binding.buttonSave.visibility = View.GONE
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		rtManager.onSaveInstanceState(outState)
	}

	override fun onPause() {
		if (!isNoteDeleted) {
			val currentTitle = title.getText(RTFormat.HTML).trim()
			val currentContent = note.getText(RTFormat.HTML).trim()
			if (currentTitle != lastSavedTitle || currentContent != lastSavedContent) {
				saveNote(noteId)
				lastSavedTitle = currentTitle
				lastSavedContent = currentContent
				hasChanges = false
			}
		}
		super.onPause()
	}

	override fun onDestroy() {
		if (!isNoteDeleted) {
			val currentTitle = title.getText(RTFormat.HTML).trim()
			val currentContent = note.getText(RTFormat.HTML).trim()
			if (currentTitle != lastSavedTitle || currentContent != lastSavedContent) {
				saveNote(noteId)
				lastSavedTitle = currentTitle
				lastSavedContent = currentContent
				hasChanges = false
			}
		}
		super.onDestroy()
	}
}
