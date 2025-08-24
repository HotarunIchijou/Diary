package org.kaorun.diary.ui.adapters

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import org.kaorun.diary.data.ThemePreview
import org.kaorun.diary.databinding.ThemeSwitchItemBinding

class ThemeAdapter(
    private var themes: List<ThemePreview>,
    private val prefs: SharedPreferences,
    private val onSchemeSelected: (Int) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ViewHolder>() {

    private var selectedIndex: Int = prefs.getInt("color_scheme", 0)

    init {
        updateSelection()
    }

    inner class ViewHolder(
        private val binding: ThemeSwitchItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(item: ThemePreview) {
            binding.topColor.setBackgroundColor(item.colorTop)
            binding.bottomLeftColor.setBackgroundColor(item.colorBottomLeft)
            binding.bottomRightColor.setBackgroundColor(item.colorBottomRight)

            binding.root.strokeColor = if (item.isSelected) MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorSecondary
            ) else Color.TRANSPARENT

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectedIndex = position
                    prefs.edit { putInt("color_scheme", selectedIndex) }
                    updateSelection()
                    notifyDataSetChanged()
                    onSchemeSelected(selectedIndex)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ThemeSwitchItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(themes[position])
    }

    override fun getItemCount(): Int = themes.size

    private fun updateSelection() {
        themes = themes.mapIndexed { index, theme ->
            theme.copy(isSelected = index == selectedIndex)
        }
    }
}
