package org.kaorun.diary.ui.activities

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import org.kaorun.diary.ui.utils.FloatingToolbarHelper
import org.kaorun.diary.ui.utils.InsetsHandler

class NoteActivity : AppCompatActivity() {

	private lateinit var binding: ActivityNoteBinding
	private lateinit var databaseRef: DatabaseReference
	private lateinit var auth: FirebaseAuth
	private lateinit var rootLayout: CoordinatorLayout
	private lateinit var rtManager: RTManager
	private lateinit var rtEditText: RTEditText
	private var noteId: String? = null
	private var isNoteDeleted: Boolean = false


	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		binding = ActivityNoteBinding.inflate(layoutInflater)
		setContentView(binding.root)

		InsetsHandler.applyAppBarInsets(binding.appBarLayout) // Applying top app bar insets, big thanks to Shemmy (and nift4, I suppose)
		
		val fab = binding.fab

		// Initialize RTManager
		val rtApi = RTApi(this, RTProxyImpl(this), RTMediaFactoryImpl(this, true))
		rtManager = RTManager(rtApi, savedInstanceState)

		// Find RTEditText and register it
		rtEditText = binding.editText
		rtManager.registerEditor(rtEditText, true)

		val toolbarHelper = FloatingToolbarHelper(rtManager, binding)
		toolbarHelper.setupFloatingToolbar()

		noteId = intent.getStringExtra("NOTE_ID")
		val noteContent = intent.getStringExtra("NOTE_CONTENT")

		if (noteContent != null) {
			rtEditText.setRichTextEditing(true, noteContent)
		}

		// Automatically focus the EditText
		binding.editText.requestFocus()

		// Show the soft keyboard
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.showSoftInput(binding.editText, InputMethodManager.SHOW_IMPLICIT)

		registerEvents(noteId)

		fab.hide()

		rootLayout = binding.root
		rootLayout.viewTreeObserver.addOnPreDrawListener {
			val rect = android.graphics.Rect()
			rootLayout.getWindowVisibleDisplayFrame(rect)
			val screenHeight = rootLayout.height
			val keypadHeight = screenHeight - rect.bottom

			if (keypadHeight > 0) {
				// Keyboard is shown, adjust the FAB position
				binding.floatingToolbarParent.translationY = -keypadHeight.toFloat()
			} else {
				// Keyboard is hidden, reset FAB position
				binding.floatingToolbarParent.translationY = 100f
			}
			true
		}
	}

	private fun registerEvents(noteId: String?) {
		binding.editText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
				if (p0.toString().trim().isNotEmpty()) binding.fab.show()
				else binding.fab.hide()
			}

			override fun afterTextChanged(p0: Editable?) {
			}
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

		binding.fab.setOnClickListener {
			saveNote(noteId)
		}
	}

	private fun saveNote(noteId: String?) {
		val note = rtEditText.getText(RTFormat.HTML)

		auth = FirebaseAuth.getInstance()
		databaseRef = FirebaseDatabase.getInstance()
			.reference.child("Notes").child(auth.currentUser?.uid.toString())

		if (noteId != null) {
			// Update existing note
			databaseRef.child(noteId).setValue(note).addOnCompleteListener {
				if (it.isSuccessful) {
					this.finish()
				} else {
					Toast.makeText(applicationContext, it.exception?.message, Toast.LENGTH_SHORT).show()
				}
			}
		} else {
			// Create new note
			if (note.isNotEmpty()) {
				databaseRef.push().setValue(note).addOnCompleteListener {
					if (it.isSuccessful) {
						binding.editText.text = null
						this.finish()
					} else {
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
					isNoteDeleted = true // Mark the note as deleted
					finish()
				} else {
					Toast.makeText(this, "Failed to delete note: ${task.exception?.message}",
						Toast.LENGTH_SHORT).show()
				}
			}
		} else {
			Toast.makeText(this, "Note not found", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		rtManager.onSaveInstanceState(outState)
	}

	override fun onPause() {
		if (!isNoteDeleted) {
			saveNote(noteId)
		}
		super.onPause()
	}

}
