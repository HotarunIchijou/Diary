package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
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
import org.kaorun.diary.viewmodel.NoteViewModel

class NoteActivity : BaseActivity() {

    private lateinit var binding: ActivityNoteBinding
    private lateinit var rtManager: RTManager
    private lateinit var title: RTEditText
    private lateinit var note: RTEditText
    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupRichTextEditor(savedInstanceState)
        setupToolbar()
        setupObservers()
        setupInputListeners()

        handleIntent()
    }

    private fun setupInsets() {
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
        InsetsHandler.applyViewInsets(binding.noteTitle, ignoreBottomPadding = true)
        InsetsHandler.applyDividerInsets(binding.titleDivider)
        InsetsHandler.applyViewInsets(binding.noteContent)
    }

    private fun setupRichTextEditor(savedInstanceState: Bundle?) {
        val rtApi = RTApi(this, RTProxyImpl(this), RTMediaFactoryImpl(this, true))
        rtManager = RTManager(rtApi, savedInstanceState)

        title = binding.noteTitle
        note = binding.noteContent

        rtManager.registerEditor(title, true)
        rtManager.registerEditor(note, true)

        title.fontVariationSettings = "'ROND' 100, 'GRAD' 100"

        FloatingToolbarHelper(rtManager, binding).setupFloatingToolbar()

        binding.buttonSave.visibility = View.GONE
        title.requestFocus()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.delete -> {
                    viewModel.deleteNote()
                    true
                }
                else -> false
            }
        }

        binding.buttonSave.setOnClickListener {
            viewModel.saveNote(
                title.getText(RTFormat.HTML),
                note.getText(RTFormat.HTML),
                closeAfter = true
            )
        }
    }

    private fun setupObservers() {
        viewModel.showSaveButton.observe(this) { visible ->
            if (visible) showSaveButton() else hideSaveButton()
        }

        viewModel.noteLoaded.observe(this) { (titleText, contentText) ->
            title.setRichTextEditing(true, titleText)
            note.setRichTextEditing(true, contentText)
        }

        viewModel.closeScreen.observe(this) {
            if (it == true) finish()
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupInputListeners() {
        title.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                notifyTextChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (title.lineCount > 5) {
                    val currentText = title.text?.toString().orEmpty()
                    title.setText(currentText.dropLast(count))
                    title.text?.let { title.setSelection(it.length) }
                }
            }
        })

        note.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                notifyTextChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun notifyTextChanged() {
        viewModel.onTextChanged(
            title.getText(RTFormat.HTML),
            note.getText(RTFormat.HTML)
        )
    }

    private fun handleIntent() {
        if (Intent.ACTION_SEND == intent.action && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                note.setRichTextEditing(true, it)
            }
        } else {
            val noteId = intent.getStringExtra("NOTE_ID")
            viewModel.setNoteId(noteId)
        }
    }

    private fun showSaveButton() {
        if (binding.buttonSave.isVisible) return
        TransitionManager.beginDelayedTransition(
            binding.root,
            MaterialFade().apply { duration = 150L }
        )
        binding.buttonSave.isVisible = true
    }

    private fun hideSaveButton() {
        if (!binding.buttonSave.isVisible) return
        TransitionManager.beginDelayedTransition(
            binding.root,
            MaterialFade().apply { duration = 84L }
        )
        binding.buttonSave.isVisible = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        rtManager.onSaveInstanceState(outState)
    }

    override fun onPause() {
        viewModel.autoSave(
            title.getText(RTFormat.HTML),
            note.getText(RTFormat.HTML)
        )
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.autoSave(
            title.getText(RTFormat.HTML),
            note.getText(RTFormat.HTML)
        )
        super.onDestroy()
    }
}
