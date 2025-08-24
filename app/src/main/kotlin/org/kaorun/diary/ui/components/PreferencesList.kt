package org.kaorun.diary.ui.components

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
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

        binding.title.text = title
        binding.summary.text = summary
        binding.icon.setImageDrawable(icon)

        binding.listItemLayout.updateAppearance(1, 1)
    }
}
