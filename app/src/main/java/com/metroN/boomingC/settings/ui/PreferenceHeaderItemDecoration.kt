package com.metroN.boomingC.settings.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.BackportMaterialDividerItemDecoration
import com.metroN.boomingC.R

/**
 * A [BackportMaterialDividerItemDecoration] that sets up the divider configuration to correctly
 * separate preference categories.
 */
class PreferenceHeaderItemDecoration
@JvmOverloads
constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialDividerStyle,
    orientation: Int = LinearLayoutManager.VERTICAL
) : BackportMaterialDividerItemDecoration(context, attributeSet, defStyleAttr, orientation) {
    @SuppressLint("RestrictedApi")
    override fun shouldDrawDivider(position: Int, adapter: RecyclerView.Adapter<*>?) =
        try {
            // Add a divider if the next item is a header (in this case a preference category
            // that corresponds to a header viewholder). This comanizes the divider to separate
            // the ends of content rather than the beginning of content, alongside an added benefit
            // of preventing top headers from having a divider applied.
            (adapter as PreferenceGroupAdapter).getItem(position + 1) is PreferenceCategory
        } catch (e: ClassCastException) {
            false
        } catch (e: IndexOutOfBoundsException) {
            false
        }
}
