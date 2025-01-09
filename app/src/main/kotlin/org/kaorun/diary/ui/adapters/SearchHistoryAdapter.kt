package org.kaorun.diary.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.R

class SearchHistoryAdapter (
	private val suggestions: MutableList<String>,
	var onItemClicked: (String) -> Unit,
	var onItemDeleted: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val suggestionText: TextView = itemView.findViewById(R.id.suggestion_text)
		private val deleteButton: ImageView = itemView.findViewById(R.id.delete)

		fun bind(query: String) {
			suggestionText.text = query
			// When item is clicked
			itemView.setOnClickListener { onItemClicked(query) }

			// When delete button is clicked
			deleteButton.setOnClickListener {
				onItemDeleted(query) // Notify the delete callback
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_search_suggestion, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val suggestion = suggestions[position]
		holder.bind(suggestion)
	}

	override fun getItemCount(): Int = suggestions.size

	fun updateSuggestions(newSuggestions: List<String>) {
		suggestions.clear()
		suggestions.addAll(newSuggestions)
		notifyDataSetChanged()
	}
}
