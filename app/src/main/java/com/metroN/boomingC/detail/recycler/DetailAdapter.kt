package com.metroN.boomingC.detail.recycler

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.databinding.ItemSortHeaderBinding
import com.metroN.boomingC.list.BasicHeader
import com.metroN.boomingC.list.Header
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.list.SelectableListListener
import com.metroN.boomingC.list.adapter.*
import com.metroN.boomingC.list.recycler.*
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.util.context
import com.metroN.boomingC.util.inflater

/**
 * A [RecyclerView.Adapter] that implements behavior shared across each detail view's adapters.
 * @param listener A [Listener] to bind interactions to.
 * @param diffCallback A [DiffUtil.ItemCallback] to use for item comparison when diffing the
 * internal list.
 */
abstract class DetailAdapter(
    private val listener: Listener<*>,
    diffCallback: DiffUtil.ItemCallback<Item>
) :
    SelectionIndicatorAdapter<Item, BasicListInstructions, RecyclerView.ViewHolder>(
        ListDiffer.Async(diffCallback)),
    AuxioRecyclerView.SpanSizeLookup {

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            // Implement support for headers and sort headers
            is BasicHeader -> BasicHeaderViewHolder.VIEW_TYPE
            is SortHeader -> SortHeaderViewHolder.VIEW_TYPE
            else -> super.getItemViewType(position)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            BasicHeaderViewHolder.VIEW_TYPE -> BasicHeaderViewHolder.from(parent)
            SortHeaderViewHolder.VIEW_TYPE -> SortHeaderViewHolder.from(parent)
            else -> error("Invalid item type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is BasicHeader -> (holder as BasicHeaderViewHolder).bind(item)
            is SortHeader -> (holder as SortHeaderViewHolder).bind(item, listener)
        }
    }

    override fun isItemFullWidth(position: Int): Boolean {
        // Headers should be full-width in all configurations.
        val item = getItem(position)
        return item is BasicHeader || item is SortHeader
    }

    /** An extended [SelectableListListener] for [DetailAdapter] implementations. */
    interface Listener<in T : Music> : SelectableListListener<T> {
        // TODO: Split off into sub-listeners if a collapsing toolbar is implemented.
        /**
         * Called when the play button in a detail header is pressed, requesting that the current
         * item should be played.
         */
        fun onPlay()

        /**
         * Called when the shuffle button in a detail header is pressed, requesting that the current
         * item should be shuffled
         */
        fun onShuffle()

        /**
         * Called when the button in a [SortHeader] item is pressed, requesting that the sort menu
         * should be opened.
         */
        fun onOpenSortMenu(anchor: View)
    }

    protected companion object {
        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Item>() {
                override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                    return when {
                        oldItem is BasicHeader && newItem is BasicHeader ->
                            BasicHeaderViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is SortHeader && newItem is SortHeader ->
                            SortHeaderViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        else -> false
                    }
                }
            }
    }
}

/**
 * A header variation that displays a button to open a sort menu.
 * @param titleRes The string resource to use as the header title
 * @author Alexander Capehart (OxygenCobalt)
 */
data class SortHeader(@StringRes override val titleRes: Int) : Header

/**
 * A [RecyclerView.ViewHolder] that displays a [SortHeader], a variation on [BasicHeader] that adds
 * a button opening a menu for sorting. Use [from] to create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class SortHeaderViewHolder(private val binding: ItemSortHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param sortHeader The new [SortHeader] to bind.
     * @param listener An [DetailAdapter.Listener] to bind interactions to.
     */
    fun bind(sortHeader: SortHeader, listener: DetailAdapter.Listener<*>) {
        binding.headerTitle.text = binding.context.getString(sortHeader.titleRes)
        binding.headerButton.apply {
            // Add a Tooltip based on the content description so that the purpose of this
            // button can be clear.
            TooltipCompat.setTooltipText(this, contentDescription)
            setOnClickListener(listener::onOpenSortMenu)
        }
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_SORT_HEADER

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            SortHeaderViewHolder(ItemSortHeaderBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<SortHeader>() {
                override fun areContentsTheSame(oldItem: SortHeader, newItem: SortHeader) =
                    oldItem.titleRes == newItem.titleRes
            }
    }
}
