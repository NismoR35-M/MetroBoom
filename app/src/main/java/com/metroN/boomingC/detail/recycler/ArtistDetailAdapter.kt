package com.metroN.boomingC.detail.recycler

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.ItemDetailBinding
import com.metroN.boomingC.databinding.ItemParentBinding
import com.metroN.boomingC.databinding.ItemSongBinding
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.list.SelectableListListener
import com.metroN.boomingC.list.adapter.SelectionIndicatorAdapter
import com.metroN.boomingC.list.adapter.SimpleDiffCallback
import com.metroN.boomingC.music.Album
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.util.context
import com.metroN.boomingC.util.getPlural
import com.metroN.boomingC.util.inflater

/**
 * A [DetailAdapter] implementing the header and sub-items for the [Artist] detail view.
 * @param listener A [DetailAdapter.Listener] to bind interactions to.
 */
class ArtistDetailAdapter(private val listener: Listener<Music>) :
    DetailAdapter(listener, DIFF_CALLBACK) {
    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            // Support an artist header, and special artist albums/songs.
            is Artist -> ArtistDetailViewHolder.VIEW_TYPE
            is Album -> ArtistAlbumViewHolder.VIEW_TYPE
            is Song -> ArtistSongViewHolder.VIEW_TYPE
            else -> super.getItemViewType(position)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            ArtistDetailViewHolder.VIEW_TYPE -> ArtistDetailViewHolder.from(parent)
            ArtistAlbumViewHolder.VIEW_TYPE -> ArtistAlbumViewHolder.from(parent)
            ArtistSongViewHolder.VIEW_TYPE -> ArtistSongViewHolder.from(parent)
            else -> super.onCreateViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        // Re-binding an item with new data and not just a changed selection/playing state.
        when (val item = getItem(position)) {
            is Artist -> (holder as ArtistDetailViewHolder).bind(item, listener)
            is Album -> (holder as ArtistAlbumViewHolder).bind(item, listener)
            is Song -> (holder as ArtistSongViewHolder).bind(item, listener)
        }
    }

    override fun isItemFullWidth(position: Int): Boolean {
        if (super.isItemFullWidth(position)) {
            return true
        }
        // Artist headers should be full-width in all configurations.
        return getItem(position) is Artist
    }

    private companion object {
        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Item>() {
                override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                    return when {
                        oldItem is Artist && newItem is Artist ->
                            ArtistDetailViewHolder.DIFF_CALLBACK.areContentsTheSame(
                                oldItem, newItem)
                        oldItem is Album && newItem is Album ->
                            ArtistAlbumViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is Song && newItem is Song ->
                            ArtistSongViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        else -> DetailAdapter.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                    }
                }
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays the [Artist] header in the detail view. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class ArtistDetailViewHolder private constructor(private val binding: ItemDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind new data to this instance.
     * @param artist The new [Artist] to bind.
     * @param listener A [DetailAdapter.Listener] to bind interactions to.
     */
    fun bind(artist: Artist, listener: DetailAdapter.Listener<*>) {
        binding.detailCover.bind(artist)
        binding.detailType.text = binding.context.getString(R.string.lbl_artist)
        binding.detailName.text = artist.resolveName(binding.context)

        if (artist.songs.isNotEmpty()) {
            // Information about the artist's genre(s) map to the sub-head text
            binding.detailSubhead.apply {
                isVisible = true
                text = artist.resolveGenreContents(binding.context)
            }

            // Song and album counts map to the info
            binding.detailInfo.text =
                binding.context.getString(
                    R.string.fmt_two,
                    binding.context.getPlural(R.plurals.fmt_album_count, artist.albums.size),
                    binding.context.getPlural(R.plurals.fmt_song_count, artist.songs.size))

            // In the case that this header used to he configured to have no songs,
            // we want to reset the visibility of all information that was hidden.
            binding.detailPlayButton.isVisible = true
            binding.detailShuffleButton.isVisible = true
        } else {
            // The artist does not have any songs, so hide functionality that makes no sense.
            // ex. Play and Shuffle, Song Counts, and Genre Information.
            // Artists are always guaranteed to have albums however, so continue to show those.
            binding.detailSubhead.isVisible = false
            binding.detailInfo.text =
                binding.context.getPlural(R.plurals.fmt_album_count, artist.albums.size)
            binding.detailPlayButton.isVisible = false
            binding.detailShuffleButton.isVisible = false
        }

        binding.detailPlayButton.setOnClickListener { listener.onPlay() }
        binding.detailShuffleButton.setOnClickListener { listener.onShuffle() }
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_ARTIST_DETAIL

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            ArtistDetailViewHolder(ItemDetailBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Artist>() {
                override fun areContentsTheSame(oldItem: Artist, newItem: Artist) =
                    oldItem.rawName == newItem.rawName &&
                        oldItem.areGenreContentsTheSame(newItem) &&
                        oldItem.albums.size == newItem.albums.size &&
                        oldItem.songs.size == newItem.songs.size
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays an [Album] in the context of an [Artist]. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class ArtistAlbumViewHolder private constructor(private val binding: ItemParentBinding) :
    SelectionIndicatorAdapter.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param album The new [Album] to bind.
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    fun bind(album: Album, listener: SelectableListListener<Album>) {
        listener.bind(album, this, menuButton = binding.parentMenu)
        binding.parentImage.bind(album)
        binding.parentName.text = album.resolveName(binding.context)
        binding.parentInfo.text =
            // Fall back to a friendlier "No date" text if the album doesn't have date information
            album.dates?.resolveDate(binding.context)
                ?: binding.context.getString(R.string.def_date)
    }

    override fun updatePlayingIndicator(isActive: Boolean, isPlaying: Boolean) {
        binding.root.isSelected = isActive
        binding.parentImage.isPlaying = isPlaying
    }

    override fun updateSelectionIndicator(isSelected: Boolean) {
        binding.root.isActivated = isSelected
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_ARTIST_ALBUM

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            ArtistAlbumViewHolder(ItemParentBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Album>() {
                override fun areContentsTheSame(oldItem: Album, newItem: Album) =
                    oldItem.rawName == newItem.rawName && oldItem.dates == newItem.dates
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays a [Song] in the context of an [Artist]. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class ArtistSongViewHolder private constructor(private val binding: ItemSongBinding) :
    SelectionIndicatorAdapter.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param song The new [Song] to bind.
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    fun bind(song: Song, listener: SelectableListListener<Song>) {
        listener.bind(song, this, menuButton = binding.songMenu)
        binding.songAlbumCover.bind(song)
        binding.songName.text = song.resolveName(binding.context)
        binding.songInfo.text = song.album.resolveName(binding.context)
    }

    override fun updatePlayingIndicator(isActive: Boolean, isPlaying: Boolean) {
        binding.root.isSelected = isActive
        binding.songAlbumCover.isPlaying = isPlaying
    }

    override fun updateSelectionIndicator(isSelected: Boolean) {
        binding.root.isActivated = isSelected
    }

    companion object {
        /** Unique ID for this ViewHolder type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_ARTIST_SONG

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            ArtistSongViewHolder(ItemSongBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Song>() {
                override fun areContentsTheSame(oldItem: Song, newItem: Song) =
                    oldItem.rawName == newItem.rawName &&
                        oldItem.album.rawName == newItem.album.rawName
            }
    }
}
