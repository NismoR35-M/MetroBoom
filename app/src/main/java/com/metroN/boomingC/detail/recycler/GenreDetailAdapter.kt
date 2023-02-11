package com.metroN.boomingC.detail.recycler

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.ItemDetailBinding
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.list.adapter.SimpleDiffCallback
import com.metroN.boomingC.list.recycler.ArtistViewHolder
import com.metroN.boomingC.list.recycler.SongViewHolder
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.util.context
import com.metroN.boomingC.util.getPlural
import com.metroN.boomingC.util.inflater

/**
 * An [DetailAdapter] implementing the header and sub-items for the [Genre] detail view.
 * @param listener A [DetailAdapter.Listener] to bind interactions to.
 */
class GenreDetailAdapter(private val listener: Listener<Music>) :
    DetailAdapter(listener, DIFF_CALLBACK) {
    override fun getItemViewType(position: Int) =
        when (getItem(position)) {
            // Support the Genre header and generic Artist/Song items. There's nothing about
            // a genre that will make the artists/songs specially formatted, so it doesn't matter
            // what we use for their ViewHolders.
            is Genre -> GenreDetailViewHolder.VIEW_TYPE
            is Artist -> ArtistViewHolder.VIEW_TYPE
            is Song -> SongViewHolder.VIEW_TYPE
            else -> super.getItemViewType(position)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            GenreDetailViewHolder.VIEW_TYPE -> GenreDetailViewHolder.from(parent)
            ArtistViewHolder.VIEW_TYPE -> ArtistViewHolder.from(parent)
            SongViewHolder.VIEW_TYPE -> SongViewHolder.from(parent)
            else -> super.onCreateViewHolder(parent, viewType)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        when (val item = getItem(position)) {
            is Genre -> (holder as GenreDetailViewHolder).bind(item, listener)
            is Artist -> (holder as ArtistViewHolder).bind(item, listener)
            is Song -> (holder as SongViewHolder).bind(item, listener)
        }
    }

    override fun isItemFullWidth(position: Int): Boolean {
        if (super.isItemFullWidth(position)) {
            return true
        }
        // Genre headers should be full-width in all configurations
        return getItem(position) is Genre
    }

    private companion object {
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Item>() {
                override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                    return when {
                        oldItem is Genre && newItem is Genre ->
                            GenreDetailViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is Artist && newItem is Artist ->
                            ArtistViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        oldItem is Song && newItem is Song ->
                            SongViewHolder.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        else -> DetailAdapter.DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                    }
                }
            }
    }
}

/**
 * A [RecyclerView.ViewHolder] that displays the [Genre] header in the detail view. Use [from] to
 * create an instance.
 * @author Alexander Capehart (OxygenCobalt)
 */
private class GenreDetailViewHolder private constructor(private val binding: ItemDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param genre The new [Song] to bind.
     * @param listener A [DetailAdapter.Listener] to bind interactions to.
     */
    fun bind(genre: Genre, listener: DetailAdapter.Listener<*>) {
        binding.detailCover.bind(genre)
        binding.detailType.text = binding.context.getString(R.string.lbl_genre)
        binding.detailName.text = genre.resolveName(binding.context)
        // Nothing about a genre is applicable to the sub-head text.
        binding.detailSubhead.isVisible = false
        // The song count of the genre maps to the info text.
        binding.detailInfo.text =
            binding.context.getString(
                R.string.fmt_two,
                binding.context.getPlural(R.plurals.fmt_artist_count, genre.artists.size),
                binding.context.getPlural(R.plurals.fmt_song_count, genre.songs.size))
        binding.detailPlayButton.setOnClickListener { listener.onPlay() }
        binding.detailShuffleButton.setOnClickListener { listener.onShuffle() }
    }

    companion object {
        /** A unique ID for this [RecyclerView.ViewHolder] type. */
        const val VIEW_TYPE = IntegerTable.VIEW_TYPE_GENRE_DETAIL

        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            GenreDetailViewHolder(ItemDetailBinding.inflate(parent.context.inflater))

        /** A comparator that can be used with DiffUtil. */
        val DIFF_CALLBACK =
            object : SimpleDiffCallback<Genre>() {
                override fun areContentsTheSame(oldItem: Genre, newItem: Genre) =
                    oldItem.rawName == newItem.rawName &&
                        oldItem.songs.size == newItem.songs.size &&
                        oldItem.durationMs == newItem.durationMs
            }
    }
}
