package com.metroN.boomingC.list.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.music.Music

/**
 * A [PlayingIndicatorAdapter] that also supports indicating the selection status of a group of
 * items.
 * @param differFactory The [ListDiffer.Factory] that defines the type of [ListDiffer] to use.
 */
abstract class SelectionIndicatorAdapter<T, I, VH : RecyclerView.ViewHolder>(
    differFactory: ListDiffer.Factory<T, I>
) : PlayingIndicatorAdapter<T, I, VH>(differFactory) {
    private var selectedItems = setOf<T>()

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (holder is ViewHolder) {
            holder.updateSelectionIndicator(selectedItems.contains(currentList[position]))
        }
    }

    /**
     * Update the list of selected items.
     * @param items A set of selected [T] items.
     */
    fun setSelected(items: Set<T>) {
        val oldSelectedItems = selectedItems
        val newSelectedItems = items.toSet()
        if (newSelectedItems == oldSelectedItems) {
            // Nothing to do.
            return
        }

        selectedItems = newSelectedItems
        for (i in currentList.indices) {
            // TODO: Perhaps add an optimization that allows me to avoid the O(n) iteration
            //  assuming all list items are unique?
            val item = currentList[i]
            if (item !is Music) {
                // Not applicable.
                continue
            }

            // Only update items that were added or removed from the list.
            val added = !oldSelectedItems.contains(item) && newSelectedItems.contains(item)
            val removed = oldSelectedItems.contains(item) && !newSelectedItems.contains(item)
            if (added || removed) {
                notifyItemChanged(i, PAYLOAD_SELECTION_INDICATOR_CHANGED)
            }
        }
    }

    /** A [PlayingIndicatorAdapter.ViewHolder] that can display a selection indicator. */
    abstract class ViewHolder(root: View) : PlayingIndicatorAdapter.ViewHolder(root) {
        /**
         * Update the selection indicator within this [PlayingIndicatorAdapter.ViewHolder].
         * @param isSelected Whether this [PlayingIndicatorAdapter.ViewHolder] is selected.
         */
        abstract fun updateSelectionIndicator(isSelected: Boolean)
    }

    private companion object {
        val PAYLOAD_SELECTION_INDICATOR_CHANGED = Any()
    }
}
