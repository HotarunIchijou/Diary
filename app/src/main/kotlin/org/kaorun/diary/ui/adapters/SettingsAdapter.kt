package org.kaorun.diary.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.listitem.ListItemViewHolder
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ItemSettingBinding

class SettingsAdapter(
    private val items: List<SettingsItem>,
    private val onItemClick: (SettingsItem) -> Unit
) : RecyclerView.Adapter<ListItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val binding = ItemSettingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ListItemViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        holder.bind(position, itemCount)

        val binding = ItemSettingBinding.bind(holder.itemView)
        val item = items[position]

        binding.title.text = item.title
        binding.summary.text = item.summary

        if (item.icon != null) {
            binding.icon.setImageResource(item.icon)
            binding.icon.isVisible = true
        } else {
            binding.icon.isVisible = false
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
