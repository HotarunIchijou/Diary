package org.kaorun.diary.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.listitem.SwipeableListItem
import org.kaorun.diary.data.NotesDatabase
import org.kaorun.diary.databinding.ItemNoteBinding

class NotesAdapter(
    private var notes: MutableList<NotesDatabase>,
    private val onItemClicked: (noteId: String, noteTitle: String, noteContent: String) -> Unit,
    private val onSelectionChanged: (isSelectionModeActive: Boolean) -> Unit,
    private val onDeleteClicked: (noteId: String) -> Unit
) : RecyclerView.Adapter<ListItemViewHolder>() {

    private val selectedNotes = mutableSetOf<String>()
    var isSelectionModeActive = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListItemViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(0, 1)

        val binding = ItemNoteBinding.bind(holder.itemView)
        val note = notes[position]
        val isSelected = selectedNotes.contains(note.id)

        binding.listItem.isChecked = isSelected

        binding.noteTitle.text = HtmlCompat.fromHtml(
            note.title.ifEmpty { note.note },
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.listItem.setOnClickListener {
            if (isSelectionModeActive) {
                toggleSelection(note.id, position)
            } else {
                onItemClicked(note.id, note.title, note.note)
            }
        }

        binding.listItem.setOnLongClickListener {
            if (!isSelectionModeActive) {
                isSelectionModeActive = true
            }
            toggleSelection(note.id, position)
            true
        }

        binding.deleteButton.setOnClickListener {
            binding.listItemLayout.swipeState = SwipeableListItem.STATE_CLOSED
            onDeleteClicked(note.id)
        }
    }

    override fun getItemCount(): Int = notes.size

    fun toggleSelection(noteId: String, position: Int) {
        if (selectedNotes.contains(noteId)) {
            selectedNotes.remove(noteId)
        } else {
            selectedNotes.add(noteId)
        }

        notifyItemChanged(position)

        isSelectionModeActive = selectedNotes.isNotEmpty()
        onSelectionChanged(isSelectionModeActive)
    }

    fun clearSelection() {
        val previouslySelected = selectedNotes.toList()
        selectedNotes.clear()
        isSelectionModeActive = false

        previouslySelected.forEach { id ->
            val pos = notes.indexOfFirst { it.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }

        onSelectionChanged(false)
    }

    fun getSelectedNotes(): List<String> = selectedNotes.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateNotes(newNotes: MutableList<NotesDatabase>) {
        notes = newNotes.asReversed()
        notifyDataSetChanged()
    }
}
