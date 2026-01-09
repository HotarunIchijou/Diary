package org.kaorun.diary.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.kaorun.diary.utils.ConvertUtils.toPx

class SpaceItemDecoration(
    private val spanCount: Int = 1,
    private val spacingPx: Int = 2.toPx()
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        if (spanCount == 1) {
            outRect.left = 0
            outRect.right = 0
            if (position != 0) outRect.top = spacingPx
            outRect.bottom = 0
            return
        }

        var column = position % spanCount
        val layoutParams = view.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            column = layoutParams.spanIndex
        }

        outRect.left = column * spacingPx / spanCount
        outRect.right = spacingPx - (column + 1) * spacingPx / spanCount

        if (position >= spanCount) {
            outRect.top = spacingPx
        }
    }
}
