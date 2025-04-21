package org.kaorun.diary.ui.adapters

import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import org.kaorun.diary.R
import org.kaorun.diary.data.ThemePreview

class ThemeAdapter(
    private var themes: List<ThemePreview>,
    private val prefs: SharedPreferences,
    private val onSchemeSelected: (Int) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    private var selectedIndex: Int = prefs.getInt("color_scheme", 0)

    init {
        updateSelection()
    }

    inner class ThemeViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.theme_switch_item, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val item = themes[position]
        val top = holder.itemView.findViewById<FrameLayout>(R.id.topColor)
        val bottomLeft = holder.itemView.findViewById<View>(R.id.bottomLeftColor)
        val bottomRight = holder.itemView.findViewById<View>(R.id.bottomRightColor)
        val card = holder.itemView as MaterialCardView

        top.setBackgroundColor(item.colorTop)
        bottomLeft.setBackgroundColor(item.colorBottomLeft)
        bottomRight.setBackgroundColor(item.colorBottomRight)

        card.strokeColor = if (item.isSelected) MaterialColors.getColor(holder.itemView,
            com.google.android.material.R.attr.colorSecondary)
        else Color.TRANSPARENT

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                selectedIndex = pos
                prefs.edit { putInt("color_scheme", selectedIndex) }
                onSchemeSelected(selectedIndex)
                notifyDataSetChanged()
            }
        }

    }

    override fun getItemCount() = themes.size

    private fun updateSelection() {
        themes = themes.mapIndexed { index, theme ->
            theme.copy(isSelected = index == selectedIndex)
        }
    }
}
