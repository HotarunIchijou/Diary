package org.kaorun.diary.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.databinding.NotesItemBinding

class NotesAdapter(
	private var notes: MutableList<NotesDatabase>,
	private val onItemClicked: (noteId: String, noteContent: String) -> Unit,
	private val onSelectionChanged: (isSelectionModeActive: Boolean) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

	private val selectedNotes = mutableSetOf<String>() // Track selected notes
	var isSelectionModeActive = false

	inner class NoteViewHolder(private val binding: NotesItemBinding) :
		RecyclerView.ViewHolder(binding.root) {

		fun bind(note: NotesDatabase, position: Int) {
			val isSelected = selectedNotes.contains(note.id)

			// Update the UI based on the selection state
			binding.CardView.isChecked = isSelected
			binding.noteTitle.text = HtmlCompat.fromHtml(note.title, HtmlCompat.FROM_HTML_MODE_COMPACT)

			// Handle click and long-click events
			binding.root.setOnClickListener {
				if (isSelectionModeActive) {
					toggleSelection(note.id, position)
				} else {
					onItemClicked(note.id, note.title)
				}
			}

			binding.root.setOnLongClickListener {
				isSelectionModeActive = true
				toggleSelection(note.id, position)
				true
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
		val binding = NotesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		return NoteViewHolder(binding)
	}

	override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
		holder.bind(notes[position], position)
	}

	override fun getItemCount(): Int = notes.size

	// Toggle selection state
	fun toggleSelection(noteId: String, position: Int) {
		if (selectedNotes.contains(noteId)) {
			selectedNotes.remove(noteId)
		} else {
			selectedNotes.add(noteId)
		}

		// Notify the changed item
		notifyItemChanged(position)

		// Notify if selection mode is active or not
		if (selectedNotes.isEmpty()) {
			isSelectionModeActive = false
		}
		onSelectionChanged(isSelectionModeActive)
	}

	// Clear all selections
	fun clearSelection() {
		val previouslySelected = selectedNotes.toList() // Track selected notes for efficient updates
		selectedNotes.clear()
		isSelectionModeActive = false

		// Update previously selected items
		previouslySelected.forEach { noteId ->
			val position = notes.indexOfFirst { it.id == noteId }
			if (position != -1) notifyItemChanged(position)
		}

		onSelectionChanged(false)
	}

	// Get selected notes
	fun getSelectedNotes(): List<String> {
		return selectedNotes.toList()
	}

	fun getNoteIdAtPosition(position: Int): String {
		return notes[position].id
	}

	fun updateNotes(newNotes: MutableList<NotesDatabase>) {
		notes = newNotes // Update the data
		notifyDataSetChanged() // Refresh the RecyclerView
	}

	fun removeItem(position: Int) {
		notes.removeAt(position)
		notifyItemRemoved(position)
	}
}
