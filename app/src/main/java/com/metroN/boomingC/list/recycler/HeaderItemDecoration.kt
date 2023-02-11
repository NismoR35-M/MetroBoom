package com.metroN.boomingC.list.recycler

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.BackportMaterialDividerItemDecoration
import com.metroN.boomingC.R
import com.metroN.boomingC.list.Header
import com.metroN.boomingC.list.adapter.DiffAdapter

/**
 * A [BackportMaterialDividerItemDecoration] that sets up the divider configuration to correctly
 * separate content with headers.
 */
class HeaderItemDecoration
@JvmOverloads
constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialDividerStyle,
    orientation: Int = LinearLayoutManager.VERTICAL
) : BackportMaterialDividerItemDecoration(context, attributeSet, defStyleAttr, orientation) {
    override fun shouldDrawDivider(position: Int, adapter: RecyclerView.Adapter<*>?) =
        try {
            // Add a divider if the next item is a header. This comanizes the divider to separate
            // the ends of content rather than the beginning of content, alongside an added benefit
            // of preventing top headers from having a divider applied.
            (adapter as DiffAdapter<*, *, *>).getItem(position + 1) is Header
        } catch (e: ClassCastException) {
            false
        } catch (e: IndexOutOfBoundsException) {
            false
        }
}
