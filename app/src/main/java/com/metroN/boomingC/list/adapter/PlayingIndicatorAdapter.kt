package com.metroN.boomingC.list.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.util.logD

/**
 * A [RecyclerView.Adapter] that supports indicating the playback status of a particular item.
 * @param differFactory The [ListDiffer.Factory] that defines the type of [ListDiffer] to use.
 */
abstract class PlayingIndicatorAdapter<T, I, VH : RecyclerView.ViewHolder>(
    differFactory: ListDiffer.Factory<T, I>
) : DiffAdapter<T, I, VH>(differFactory) {
    // There are actually two states for this adapter:
    // - The currently playing item, which is usually marked as "selected" and becomes accented.
    // - Whether playback is ongoing, which corresponds to whether the item's ImageGroup is
    // marked as "playing" or not.
    private var currentItem: T? = null
    private var isPlaying = false

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        // Only try to update the playing indicator if the ViewHolder supports it
        if (holder is ViewHolder) {
            holder.updatePlayingIndicator(currentList[position] == currentItem, isPlaying)
        }

        if (payloads.isEmpty()) {
            // Not updating any indicator-specific attributes, so delegate to the concrete
            // adapter (actually bind the item)
            onBindViewHolder(holder, position)
        }
    }
    /**
     * Update the currently playing item in the list.
     * @param item The [T] currently being played, or null if it is not being played.
     * @param isPlaying Whether playback is ongoing or paused.
     */
    fun setPlaying(item: T?, isPlaying: Boolean) {
        var updatedItem = false
        if (currentItem != item) {
            val oldItem = currentItem
            currentItem = item

            // Remove the playing indicator from the old item
            if (oldItem != null) {
                val pos = currentList.indexOfFirst { it == oldItem }
                if (pos > -1) {
                    notifyItemChanged(pos, PAYLOAD_PLAYING_INDICATOR_CHANGED)
                } else {
                    logD("oldItem was not in adapter data")
                }
            }

            // Enable the playing indicator on the new item
            if (item != null) {
                val pos = currentList.indexOfFirst { it == item }
                if (pos > -1) {
                    notifyItemChanged(pos, PAYLOAD_PLAYING_INDICATOR_CHANGED)
                } else {
                    logD("newItem was not in adapter data")
                }
            }

            updatedItem = true
        }

        if (this.isPlaying != isPlaying) {
            this.isPlaying = isPlaying

            // We may have already called notifyItemChanged before when checking
            // if the item was being played, so in that case we don't need to
            // update again here.
            if (!updatedItem && item != null) {
                val pos = currentList.indexOfFirst { it == item }
                if (pos > -1) {
                    notifyItemChanged(pos, PAYLOAD_PLAYING_INDICATOR_CHANGED)
                } else {
                    logD("newItem was not in adapter data")
                }
            }
        }
    }

    /** A [RecyclerView.ViewHolder] that can display a playing indicator. */
    abstract class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        /**
         * Update the playing indicator within this [RecyclerView.ViewHolder].
         * @param isActive True if this item is playing, false otherwise.
         * @param isPlaying True if playback is ongoing, false if paused. If this is true,
         * [isActive] will also be true.
         */
        abstract fun updatePlayingIndicator(isActive: Boolean, isPlaying: Boolean)
    }

    private companion object {
        val PAYLOAD_PLAYING_INDICATOR_CHANGED = Any()
    }
}
