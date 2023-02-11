package com.metroN.boomingC.playback.queue

import android.annotation.SuppressLint
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.MaterialShapeDrawable
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.ItemQueueSongBinding
import com.metroN.boomingC.list.EditableListListener
import com.metroN.boomingC.list.adapter.BasicListInstructions
import com.metroN.boomingC.list.adapter.DiffAdapter
import com.metroN.boomingC.list.adapter.ListDiffer
import com.metroN.boomingC.list.adapter.PlayingIndicatorAdapter
import com.metroN.boomingC.list.recycler.SongViewHolder
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.util.*

/**
 * A [RecyclerView.Adapter] that shows an editable list of queue items.
 * @param listener A [EditableListListener] to bind interactions to.
 */
class QueueAdapter(private val listener: EditableListListener<Song>) :
    DiffAdapter<Song, BasicListInstructions, QueueSongViewHolder>(
        ListDiffer.Blocking(QueueSongViewHolder.DIFF_CALLBACK)) {
    // Since PlayingIndicator adapter relies on an item value, we cannot use it for this
    // adapter, as one item can appear at several points in the UI. Use a similar implementation
    // with an index value instead.
    private var currentIndex = 0
    private var isPlaying = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        QueueSongViewHolder.from(parent)

    override fun onBindViewHolder(holder: QueueSongViewHolder, position: Int) =
        throw IllegalStateException()

    override fun onBindViewHolder(
        viewHolder: QueueSongViewHolder,
        position: Int,
        payload: List<Any>
    ) {
        if (payload.isEmpty()) {
            viewHolder.bind(getItem(position), listener)
        }

        viewHolder.isFuture = position > currentIndex
        viewHolder.updatePlayingIndicator(position == currentIndex, isPlaying)
    }

    /**
     * Set the position of the currently playing item in the queue. This will mark the item as
     * playing and any previous items as played.
     * @param index The position of the currently playing item in the queue.
     * @param isPlaying Whether playback is ongoing or paused.
     */
    fun setPosition(index: Int, isPlaying: Boolean) {
        logD("Updating index")
        val lastIndex = currentIndex
        currentIndex = index

        // Have to update not only the currently playing item, but also all items marked
        // as playing.
        if (currentIndex < lastIndex) {
            notifyItemRangeChanged(0, lastIndex + 1, PAYLOAD_UPDATE_POSITION)
        } else {
            notifyItemRangeChanged(0, currentIndex + 1, PAYLOAD_UPDATE_POSITION)
        }

        this.isPlaying = isPlaying
    }

    private companion object {
        val PAYLOAD_UPDATE_POSITION = Any()
    }
}

/**
 * A [PlayingIndicatorAdapter.ViewHolder] that displays a queue [Song]. Use [from] to create an
 * instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
class QueueSongViewHolder private constructor(private val binding: ItemQueueSongBinding) :
    PlayingIndicatorAdapter.ViewHolder(binding.root) {
    /** The "body" view of this [QueueSongViewHolder] that shows the [Song] information. */
    val bodyView: View
        get() = binding.body

    /** The background view of this [QueueSongViewHolder] that shows the delete icon. */
    val backgroundView: View
        get() = binding.background

    /** The actual background drawable of this [QueueSongViewHolder] that can be manipulated. */
    val backgroundDrawable =
        MaterialShapeDrawable.createWithElevationOverlay(binding.root.context).apply {
            fillColor = binding.context.getAttrColorCompat(R.attr.colorSurface)
            elevation = binding.context.getDimen(R.dimen.elevation_normal) * 5
            alpha = 0
        }

    /** If this queue item is considered "in the future" (i.e has not played yet). */
    var isFuture: Boolean
        get() = binding.songAlbumCover.isEnabled
        set(value) {
            // Don't want to disable clicking, just indicate the body and handle is disabled
            binding.songAlbumCover.isEnabled = value
            binding.songName.isEnabled = value
            binding.songInfo.isEnabled = value
        }

    init {
        binding.body.background =
            LayerDrawable(
                arrayOf(
                    MaterialShapeDrawable.createWithElevationOverlay(binding.context).apply {
                        fillColor = binding.context.getAttrColorCompat(R.attr.colorSurface)
                        elevation = binding.context.getDimen(R.dimen.elevation_normal)
                    },
                    backgroundDrawable))
    }

    /**
     * Bind new data to this instance.
     * @param song The new [Song] to bind.
     * @param listener A [EditableListListener] to bind interactions to.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun bind(song: Song, listener: EditableListListener<Song>) {
        listener.bind(song, this, bodyView, binding.songDragHandle)
        binding.songAlbumCover.bind(song)
        binding.songName.text = song.resolveName(binding.context)
        binding.songInfo.text = song.resolveArtistContents(binding.context)
        // Not swiping this ViewHolder if it's being re-bound, ensure that the background is
        // not visible. See QueueDragCallback for why this is done.
        binding.background.isInvisible = true
    }

    override fun updatePlayingIndicator(isActive: Boolean, isPlaying: Boolean) {
        binding.interactBody.isSelected = isActive
        binding.songAlbumCover.isPlaying = isPlaying
    }

    companion object {
        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            QueueSongViewHolder(ItemQueueSongBinding.inflate(parent.context.inflater))

        // TODO: This is not good enough, I need to compare item indices as well.
        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK = SongViewHolder.DIFF_CALLBACK
    }
}
