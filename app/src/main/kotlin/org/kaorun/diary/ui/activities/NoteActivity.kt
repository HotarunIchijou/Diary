package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
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

class NoteActivity : BaseActivity() {

	private lateinit var binding: ActivityNoteBinding
	private lateinit var databaseRef: DatabaseReference
	private lateinit var auth: FirebaseAuth
	private lateinit var rtManager: RTManager
	private lateinit var title: RTEditText
	private lateinit var note: RTEditText
	private var noteId: String? = null
	private var isNoteDeleted: Boolean = false
	private var lastSavedNote: String? = null

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
				if (noteContent != null) {
					note.setRichTextEditing(true, noteContent)
				}
			}
		} else {
			noteId = intent.getStringExtra("NOTE_ID")
			val noteTitle = intent.getStringExtra("NOTE_TITLE")
			val noteContent = intent.getStringExtra("NOTE_CONTENT")

			if (noteContent != null) {
				title.setRichTextEditing(true, noteTitle)
				note.setRichTextEditing(true, noteContent)
			}
		}

		binding.noteTitle.requestFocus()

		registerEvents()

	}

	private fun registerEvents() {
		binding.noteTitle.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
				if (binding.noteTitle.lineCount > 5) {
					val currentText = binding.noteTitle.text?.toString() ?: ""
					binding.noteTitle.setText(currentText.substring(0, currentText.length - p3))
					binding.noteTitle.text?.let { binding.noteTitle.setSelection(it.length) }
				}
			}

			override fun afterTextChanged(p0: Editable?) {}
		})

		binding.noteContent.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun afterTextChanged(p0: Editable?) {}
		})

		binding.topAppBar.setNavigationOnClickListener {
			this.finish()
		}

		binding.topAppBar.setOnMenuItemClickListener { menuItem ->
			when (menuItem.itemId) {
				R.id.delete -> {
					deleteNote()
					true
				}
				else -> false
			}
		}
	}

	private fun saveNote(noteId: String?) {
		val titleText = title.getText(RTFormat.HTML)
		val noteText = note.getText(RTFormat.HTML)

		auth = FirebaseAuth.getInstance()
		databaseRef = FirebaseDatabase.getInstance()
			.reference.child("Notes").child(auth.currentUser?.uid.toString())

		val noteData = mapOf(
			"title" to titleText,
			"note" to noteText)

		if (noteId != null) {
			databaseRef.child(noteId).setValue(noteData).addOnCompleteListener {
				if (!it.isSuccessful) {
					Toast.makeText(applicationContext, it.exception?.message, Toast.LENGTH_SHORT).show()
				}
			}
		} else {
			if (noteText.isNotEmpty() || titleText.isNotEmpty()) {
				databaseRef.push().setValue(noteData).addOnCompleteListener {
					if (!it.isSuccessful) {
						Toast.makeText(applicationContext, it.exception?.message, Toast.LENGTH_SHORT).show()
					}
				}
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
				} else {
					Toast.makeText(this, "Failed to delete note: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
				}
			}
		} else {
			isNoteDeleted = true
			finish()
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		rtManager.onSaveInstanceState(outState)
	}

	override fun onPause() {
		if (!isNoteDeleted) {
			val currentNote = title.getText(RTFormat.HTML).trim()
			if (currentNote != lastSavedNote) {
				saveNote(noteId)
				lastSavedNote = currentNote
			}
		}
		super.onPause()
	}

	override fun onDestroy() {
		if (!isNoteDeleted) {
			val currentNote = title.getText(RTFormat.HTML).trim()
			if (currentNote != lastSavedNote) {
				saveNote(noteId)
				lastSavedNote = currentNote
			}
		}
		super.onDestroy()
	}
}
