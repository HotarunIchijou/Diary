package org.kaorun.diary.utils

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding


object InsetsHandler {
	fun applyViewInsets(view: View, additionalBottomPadding: Int = 16, isTopPadding: Boolean = false) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars()
					or WindowInsetsCompat.Type.displayCutout()
					or WindowInsetsCompat.Type.ime()
			)
			v.updatePadding(
				left = bars.left,
				right = bars.right,
				bottom = bars.bottom + additionalBottomPadding,
				top = if (isTopPadding) bars.top else v.paddingTop
			)
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyFabInsets(view: View, additionalBottomMargin: Int = 40, additionalRightMargin: Int = 16) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
					or WindowInsetsCompat.Type.displayCutout())
			v.updateLayoutParams<MarginLayoutParams> {
				marginEnd = bars.right + dpToPx(view, additionalRightMargin)
				bottomMargin = bars.bottom + additionalBottomMargin
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

	private fun dpToPx(view: View, dp: Int): Int {
		val displayMetrics: DisplayMetrics = view.resources.displayMetrics
		return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
	}
}
