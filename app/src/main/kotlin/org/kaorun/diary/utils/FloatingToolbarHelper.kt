package org.kaorun.diary.utils

import android.graphics.Typeface
import android.text.Editable
import android.text.style.StyleSpan
import android.view.textclassifier.SelectionEvent
import android.view.textclassifier.TextClassifier
import com.google.android.material.button.MaterialButton
import com.onegravity.rteditor.RTEditText
import com.onegravity.rteditor.RTManager
import com.onegravity.rteditor.effects.Effect
import com.onegravity.rteditor.effects.Effects
import com.onegravity.rteditor.spans.RTSpan
import org.kaorun.diary.databinding.ActivityNoteBinding

class FloatingToolbarHelper(
	private val rtManager: RTManager,
	private val binding: ActivityNoteBinding
) {

	private var isBold = false
	private var isItalic = false
	private var isUnderline = false

	fun setupFloatingToolbar() {
		setupStyleToggle(
			button = binding.styleBold,
			effect = Effects.BOLD,
			typeface = Typeface.BOLD,
			flagUpdater = { isBold = it },
			flagGetter = { isBold },
			styleChecker = ::isStyleApplied
		)

		setupStyleToggle(
			button = binding.styleItalic,
			effect = Effects.ITALIC,
			typeface = Typeface.ITALIC,
			flagUpdater = { isItalic = it },
			flagGetter = { isItalic },
			styleChecker = ::isStyleApplied
		)

		setupStyleToggle(
			button = binding.styleUnderline,
			effect = Effects.UNDERLINE,
			flagUpdater = { isUnderline = it },
			flagGetter = { isUnderline },
			styleChecker = { spannable, start, end, _ -> isUnderlineApplied(spannable, start, end) }
		)
	}

	private fun <T : RTSpan<Boolean>> setupStyleToggle(
		button: MaterialButton,
		effect: Effect<Boolean, T>,
		typeface: Int? = null,
		flagUpdater: (Boolean) -> Unit,
		flagGetter: () -> Boolean,
		styleChecker: (spannable: Editable?, start: Int, end: Int, typeface: Int?) -> Boolean
	) {
		button.setOnClickListener {
			val isActive = !flagGetter()
			flagUpdater(isActive)
			button.isChecked = isActive
			rtManager.onEffectSelected(effect, isActive)

			setClassifier(styleChecker, typeface, flagUpdater, button, binding.noteTitle)
			setClassifier(styleChecker, typeface, flagUpdater, button, binding.noteContent)
		}
	}

	private fun setClassifier(
		styleChecker: (spannable: Editable?, start: Int, end: Int, typeface: Int?) -> Boolean,
		typeface: Int?,
		flagUpdater: (Boolean) -> Unit,
		button: MaterialButton,
		textbox: RTEditText
	) {
		textbox.setTextClassifier(
			object : TextClassifier {
				override fun onSelectionEvent(event: SelectionEvent) {
					val spannable = textbox.text
					if (styleChecker(spannable, textbox.selectionStart, textbox.selectionEnd, typeface)) {
						flagUpdater(true)
						button.isChecked = true
					}
				}
			},
		)
	}

	// Updated style checker functions
	private fun isStyleApplied(spannable: Editable?, start: Int, end: Int, typeface: Int?): Boolean {
		val styleSpans = spannable?.getSpans(start, end, StyleSpan::class.java)
		return styleSpans?.any { it.style == typeface } == true
	}

	private fun isUnderlineApplied(spannable: Editable?, start: Int, end: Int): Boolean {
		val underlineSpans = spannable?.getSpans(start, end, com.onegravity.rteditor.spans.UnderlineSpan::class.java)
		return underlineSpans?.any { spannable.getSpanStart(it) <= end && spannable.getSpanEnd(it) >= start } == true
	}
}
