package org.kaorun.diary.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

object InsetsHandler {
	fun applyRecyclerViewInsets(view: View, additionalBottomPadding: Int = 16) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
			)
			v.updatePadding(
				left = bars.left,
				right = bars.right,
				bottom = bars.bottom + additionalBottomPadding
			)
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyFabInsets(view: View, additionalBottomMargin: Int = 40) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				bottomMargin = bars.bottom + additionalBottomMargin
			}
			WindowInsetsCompat.CONSUMED
		}
	}

	fun applyAppBarInsets(view: View) {
		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val bars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
			)
			v.updatePadding(
				left = bars.left,
				top = bars.top,
				right = bars.right
			)
			WindowInsetsCompat.CONSUMED
		}
	}
}
