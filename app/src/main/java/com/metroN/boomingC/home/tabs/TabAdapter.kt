package com.metroN.boomingC.home.tabs

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.ItemTabBinding
import com.metroN.boomingC.list.EditableListListener
import com.metroN.boomingC.list.recycler.DialogRecyclerView
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.util.inflater

/**
 * A [RecyclerView.Adapter] that displays an array of [Tab]s open for configuration.
 * @param listener A [EditableListListener] for tab interactions.
 */
class TabAdapter(private val listener: EditableListListener<Tab>) :
    RecyclerView.Adapter<TabViewHolder>() {
    /** The current array of [Tab]s. */
    var tabs = arrayOf<Tab>()
        private set

    override fun getItemCount() = tabs.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TabViewHolder.from(parent)
    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(tabs[position], listener)
    }

    /**
     * Immediately update the tab array. This should be used when initializing the list.
     * @param newTabs The new array of tabs to show.
     */
    fun submitTabs(newTabs: Array<Tab>) {
        tabs = newTabs
        @Suppress("NotifyDatasetChanged") notifyDataSetChanged()
    }

    /**
     * Update a specific tab to the given value.
     * @param at The position of the tab to update.
     * @param tab The new tab.
     */
    fun setTab(at: Int, tab: Tab) {
        tabs[at] = tab
        // Use a payload to avoid an item change animation.
        notifyItemChanged(at, PAYLOAD_TAB_CHANGED)
    }

    /**
     * Swap two tabs with each other.
     * @param a The position of the first tab to swap.
     * @param b The position of the second tab to swap.
     */
    fun swapTabs(a: Int, b: Int) {
        val tmp = tabs[b]
        tabs[b] = tabs[a]
        tabs[a] = tmp
        notifyItemMoved(a, b)
    }

    private companion object {
        val PAYLOAD_TAB_CHANGED = Any()
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays a [Tab]. Use [from] to create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
class TabViewHolder private constructor(private val binding: ItemTabBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param tab The new [Tab] to bind.
     * @param listener A [EditableListListener] to bind interactions to.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun bind(tab: Tab, listener: EditableListListener<Tab>) {
        listener.bind(tab, this, dragHandle = binding.tabDragHandle)
        binding.tabCheckBox.apply {
            // Update the CheckBox name to align with the mode
            setText(
                when (tab.mode) {
                    MusicMode.SONGS -> R.string.lbl_songs
                    MusicMode.ALBUMS -> R.string.lbl_albums
                    MusicMode.ARTISTS -> R.string.lbl_artists
                    MusicMode.GENRES -> R.string.lbl_genres
                })

            // Unlike in other adapters, we update the checked state alongside
            // the tab data since they are in the same data structure (Tab)
            isChecked = tab is Tab.Visible
        }
    }

    companion object {
        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) = TabViewHolder(ItemTabBinding.inflate(parent.context.inflater))
    }
}
