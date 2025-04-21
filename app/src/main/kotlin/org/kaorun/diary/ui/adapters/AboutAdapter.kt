package org.kaorun.diary.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ItemSettingBinding

class AboutAdapter(
    private val items: List<SettingsItem>,
    private val onUrlClick: (String) -> Unit
) : RecyclerView.Adapter<AboutAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemSettingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SettingsItem) {
            binding.title.text = item.title
            binding.summary.text = item.summary
            if (item.icon != null) binding.icon.setImageResource(item.icon) else binding.icon.isVisible = false
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                   if (item.url != null)onUrlClick(item.url)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
