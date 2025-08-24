package org.kaorun.diary.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.kaorun.diary.R

class VerticalSpaceItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val itemMargin: Int = context.resources.getDimensionPixelSize(R.dimen.list_item_margin)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position != state.itemCount - 1) {
            outRect.bottom = itemMargin
        }
    }
}
