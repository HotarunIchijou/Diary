package org.kaorun.diary.utils

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import org.kaorun.diary.utils.ConvertUtils.toPx

object InsetsHandler {
	fun applyViewInsets(
        view: View, additionalBottomPadding: Int = 16,
        isTopPadding: Boolean = false,
        ignoreBottomPadding: Boolean = false
    ) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars()
					or WindowInsetsCompat.Type.displayCutout()
					or WindowInsetsCompat.Type.ime()
			)
			v.updatePadding(
				left = bars.left,
				right = bars.right,
				bottom = if (ignoreBottomPadding) 0 else bars.bottom + additionalBottomPadding.toPx(),
				top = if (isTopPadding) bars.top else v.paddingTop
			)
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyFabInsets(fab: View, margin: Int = 16) {
		ViewCompat.setOnApplyWindowInsetsListener(fab) { v, windowInsets ->
			val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left + margin.toPx()
                bottomMargin = insets.bottom + margin.toPx()
                rightMargin = insets.right + margin.toPx()
            }
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyDividerInsets(view: View, margin: Int = 24) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
					or WindowInsetsCompat.Type.displayCutout())
			v.updateLayoutParams<MarginLayoutParams> {
				marginStart = bars.left + margin.toPx()
				marginEnd = bars.right + margin.toPx()
			}
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyAppBarInsets(view: View) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
			)
			v.updatePadding(
				left = bars.left,
				top = bars.top,
				right = bars.right,
			)
			WindowInsetsCompat.CONSUMED
		}
	}
}
