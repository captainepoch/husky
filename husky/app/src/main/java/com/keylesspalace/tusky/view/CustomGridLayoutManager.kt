package com.keylesspalace.tusky.view

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

class CustomGridLayoutManager(context: Context, columnWidth: Int) : GridLayoutManager(context, 1) {

    private var columnWidth: Int = columnWidth
        set(value) {
            if (value > 0 && value != field) {
                field = value
            }
        }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val currentWidth = width - paddingRight - paddingLeft
        if (columnWidth > 0 && currentWidth > 0) {
            val newSpanCount = max(1, currentWidth / columnWidth)
            if (spanCount != newSpanCount) {
                spanCount = newSpanCount
            }
        }
        super.onLayoutChildren(recycler, state)
    }
}
