package org.kaorun.diary.ui.components

import androidx.core.view.isVisible
import org.kaorun.diary.databinding.ItemSettingBinding

fun ItemSettingBinding.bindSetting(
    titleText: CharSequence?,
    summaryText: CharSequence?,
    iconRes: Int? = null,
    iconDrawable: android.graphics.drawable.Drawable? = null
) {
    title.fontVariationSettings = "'wght' 500, 'wdth' 94.7, 'opsz' 16"
    title.text = titleText

    summary.fontVariationSettings = "'wght' 400, 'wdth' 94.7, 'opsz' 14"
    summary.text = summaryText

    when {
        iconRes != null -> {
            icon.setImageResource(iconRes)
            iconFrame.isVisible = true
        }
        iconDrawable != null -> {
            icon.setImageDrawable(iconDrawable)
            iconFrame.isVisible = true
        }
        else -> {
            iconFrame.isVisible = false
        }
    }
}
