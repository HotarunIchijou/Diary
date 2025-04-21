package org.kaorun.diary.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.kaorun.diary.R

class PreferencesList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.item_setting
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val titleView = holder.findViewById(R.id.title) as TextView
        val summaryView = holder.findViewById(R.id.summary) as TextView
        val iconView = holder.findViewById(R.id.icon) as ImageView

        titleView.text = title
        summaryView.text = summary
        iconView.setImageDrawable(icon)
    }
}
