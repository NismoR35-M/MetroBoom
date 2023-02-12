package com.metroN.boomingC.ui.accent

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.R
import com.metroN.boomingC.util.getDimenPixels
import kotlin.math.max

/**
 * A [GridLayoutManager] that automatically sets the span size in order to use the most possible
 * space in the [RecyclerView]. Derived from this StackOverflow answer:
 * https://stackoverflow.com/a/30256880/14143986
 */
class AccentGridLayoutManager(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int,
    defStyleRes: Int
) : GridLayoutManager(context, attrs, defStyleAttr, defStyleRes) {
    // We use 56dp here since that's the rough size of the accent item.
    // This will need to be modified if this is used beyond the accent dialog.
    private var columnWidth = context.getDimenPixels(R.dimen.size_accent_item)
    private var lastWidth = -1
    private var lastHeight = -1

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (width > 0 && height > 0 && (lastWidth != width || lastHeight != height)) {
            val totalSpace = width - paddingRight - paddingLeft
            spanCount = max(1, totalSpace / columnWidth)
        }
        lastWidth = width
        lastHeight = height
        super.onLayoutChildren(recycler, state)
    }
}
