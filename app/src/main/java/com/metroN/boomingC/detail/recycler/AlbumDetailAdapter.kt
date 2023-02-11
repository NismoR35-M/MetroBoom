package com.metroN.boomingC.detail.recycler

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.ItemAlbumSongBinding
import com.metroN.boomingC.databinding.ItemDetailBinding
import com.metroN.boomingC.databinding.ItemDiscHeaderBinding
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.list.SelectableListListener
import com.metroN.boomingC.list.adapter.SelectionIndicatorAdapter
import com.metroN.boomingC.list.adapter.SimpleDiffCallback
import com.metroN.boomingC.music.Album
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.music.metadata.Disc
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.util.context
import com.metroN.boomingC.util.getPlural
import com.metroN.boomingC.util.inflater

/**
 * An [DetailAdapter] implementing the header and sub-items for the [Album] detail view.
 * @param listener A [Listener] to bind interactions to.
 */
class AlbumDetailAdapter(private val listener: Listener) : DetailAdapter(listener, DIFF_CALLBACK) {
    /**
     * An extension to [DetailAdapter.Listener] that enables interactions specific to the album
     * detail view.
     */
    interface Listener : DetailAdapter.Listener<Song> {
        /**
         * Called when the artist name in the [Album] header was clicked, requesting navigation to
         * it's parent artist.
         */
        fun onNavigateToParentArtist()
    }

    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            // Support the Album header, sub-headers for each disc, and special album songs.
            is Album -> AlbumDetailViewHolder.VIEW_TYPE
            is Disc -> DiscViewHolder.VIEW_TYPE
            is Song -> AlbumSongViewHolder.VIEW_TYPE
            else -> super.getItemViewType(position)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            AlbumDetailViewHolder.VIEW_TYPE -> AlbumDetailViewHolder.from(parent)
            DiscViewHolder.VIEW_TYPE -> DiscViewHolder.from(parent)
            AlbumSongViewHolder.VIEW_TYPE -> AlbumSongViewHolder.from(parent)
            else -> super.onCreateViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (val item = getItem(position)) {
            is Album -> (holder as AlbumDetailViewHolder).bind(item, listener)
            is Disc -> (holder as DiscViewHolder).bind(item)
            is Song -> (holder as AlbumSongViewHolder).bind(item, listener)
        }
    }

    override fun isItemFullWidth(position: Int): Boolean {
        if (super.isItemFullWidth(position)) {
            return true
        }
        // The album and disc headers should be full-width in all configurations.
        val item = getItem(position)
        return item is Album || item is Disc
    }

    private companion object {
        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Item>() {
                override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                    return when {
                        oldItem is Album && newItem is Album ->
                            AlbumDetailViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is Disc && newItem is Disc ->
                            DiscViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is Song && newItem is Song ->
                            AlbumSongViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)

                        // Fall back to DetailAdapter's differ to handle other headers.
                        else -> DetailAdapter.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                    }
                }
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays the [Album] header in the detail view. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class AlbumDetailViewHolder private constructor(private val binding: ItemDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind new data to this instance.
     * @param album The new [Album] to bind.
     * @param listener A [AlbumDetailAdapter.Listener] to bind interactions to.
     */
    fun bind(album: Album, listener: AlbumDetailAdapter.Listener) {
        binding.detailCover.bind(album)

        // The type text depends on the release type (Album, EP, Single, etc.)
        binding.detailType.text = binding.context.getString(album.releaseType.stringRes)

        binding.detailName.text = album.resolveName(binding.context)

        // Artist name maps to the subhead text
        binding.detailSubhead.apply {
            text = album.resolveArtistContents(context)

            // Add a QoL behavior where navigation to the artist will occur if the artist
            // name is pressed.
            setOnClickListener { listener.onNavigateToParentArtist() }
        }

        // Date, song count, and duration map to the info text
        binding.detailInfo.apply {
            // Fall back to a friendlier "No date" text if the album doesn't have date information
            val date = album.dates?.resolveDate(context) ?: context.getString(R.string.def_date)
            val songCount = context.getPlural(R.plurals.fmt_song_count, album.songs.size)
            val duration = album.durationMs.formatDurationMs(true)
            text = context.getString(R.string.fmt_three, date, songCount, duration)
        }

        binding.detailPlayButton.setOnClickListener { listener.onPlay() }
        binding.detailShuffleButton.setOnClickListener { listener.onShuffle() }
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_ALBUM_DETAIL

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            AlbumDetailViewHolder(ItemDetailBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Album>() {
                override fun areContentsTheSame(oldItem: Album, newItem: Album) =
                    oldItem.rawName == newItem.rawName &&
                        oldItem.areArtistContentsTheSame(newItem) &&
                        oldItem.dates == newItem.dates &&
                        oldItem.songs.size == newItem.songs.size &&
                        oldItem.durationMs == newItem.durationMs &&
                        oldItem.releaseType == newItem.releaseType
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays a [Disc] to delimit different disc groups. Use [from]
 * to create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class DiscViewHolder(private val binding: ItemDiscHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param disc The new [disc] to bind.
     */
    fun bind(disc: Disc) {
        binding.discNumber.text = binding.context.getString(R.string.fmt_disc_no, disc.number)
        binding.discName.apply {
            text = disc.name
            isGone = disc.name == null
        }
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_DISC_HEADER

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            DiscViewHolder(ItemDiscHeaderBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Disc>() {
                override fun areContentsTheSame(oldItem: Disc, newItem: Disc) =
                    oldItem.number == newItem.number && oldItem.name == newItem.name
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays a [Song] in the context of an [Album]. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class AlbumSongViewHolder private constructor(private val binding: ItemAlbumSongBinding) :
    SelectionIndicatorAdapter.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param song The new [Song] to bind.
     * @param listener A [SelectableListListener] to bind interactions to.
     */
    fun bind(song: Song, listener: SelectableListListener<Song>) {
        listener.bind(song, this, menuButton = binding.songMenu)

        binding.songTrack.apply {
            if (song.track != null) {
                // Instead of an album cover, we show the track number, as the song list
                // within the album detail view would have homogeneous album covers otherwise.
                text = context.getString(R.string.fmt_number, song.track)
                isInvisible = false
                contentDescription = context.getString(R.string.desc_track_number, song.track)
            } else {
                // No track, do not show a number, instead showing a generic icon.
                text = ""
                isInvisible = true
                contentDescription = context.getString(R.string.def_track)
            }
        }

        binding.songName.text = song.resolveName(binding.context)

        // Use duration instead of album or artist for each song, as this text would
        // be homogenous otherwise.
        binding.songDuration.text = song.durationMs.formatDurationMs(false)
    }

    override fun updatePlayingIndicator(isActive: Boolean, isPlaying: Boolean) {
        binding.root.isSelected = isActive
        binding.songTrackBg.isPlaying = isPlaying
    }

    override fun updateSelectionIndicator(isSelected: Boolean) {
        binding.root.isActivated = isSelected
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_ALBUM_SONG

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            AlbumSongViewHolder(ItemAlbumSongBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Song>() {
                override fun areContentsTheSame(oldItem: Song, newItem: Song) =
                    oldItem.rawName == newItem.rawName && oldItem.durationMs == newItem.durationMs
            }
    }
}
