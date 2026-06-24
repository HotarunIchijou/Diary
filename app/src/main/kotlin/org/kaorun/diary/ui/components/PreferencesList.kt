package org.kaorun.diary.ui.components

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.R
import org.kaorun.diary.databinding.ItemSettingBinding

class PreferencesList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.item_setting
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val binding = ItemSettingBinding.bind(holder.itemView)
        
        val recyclerView = holder.itemView.parent as? RecyclerView
        val position = recyclerView?.getChildAdapterPosition(holder.itemView) ?: 0
        val itemCount = recyclerView?.adapter?.itemCount ?: 1

        binding.bindSetting(title, summary, iconDrawable = icon)
        binding.listItemLayout.updateAppearance(position, itemCount)
    }
}
