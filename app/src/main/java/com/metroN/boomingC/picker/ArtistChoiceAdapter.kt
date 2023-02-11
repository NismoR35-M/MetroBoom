package com.metroN.boomingC.picker

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.databinding.ItemPickerChoiceBinding
import com.metroN.boomingC.list.ClickableListListener
import com.metroN.boomingC.list.recycler.DialogRecyclerView
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.util.context
import com.metroN.boomingC.util.inflater

/**
 * An [RecyclerView.Adapter] that displays a list of [Artist] choices.
 * @param listener A [ClickableListListener] to bind interactions to.
 */
class ArtistChoiceAdapter(private val listener: ClickableListListener<Artist>) :
    RecyclerView.Adapter<ArtistChoiceViewHolder>() {
    private var artists = listOf<Artist>()

    override fun getItemCount() = artists.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ArtistChoiceViewHolder.from(parent)

    override fun onBindViewHolder(holder: ArtistChoiceViewHolder, position: Int) =
        holder.bind(artists[position], listener)

    /**
     * Immediately update the [Artist] choices.
     * @param newArtists The new [Artist]s to show.
     */
    fun submitList(newArtists: List<Artist>) {
        if (newArtists != artists) {
            artists = newArtists
            @Suppress("NotifyDataSetChanged") notifyDataSetChanged()
        }
    }
}

/**
 * A [DialogRecyclerView.ViewHolder] that displays a smaller variant of a typical [Artist] item, for
 * use with [ArtistChoiceAdapter]. Use [from] to create an instance.
 */
class ArtistChoiceViewHolder(private val binding: ItemPickerChoiceBinding) :
    DialogRecyclerView.ViewHolder(binding.root) {
    /**
     * Bind new data to this instance.
     * @param artist The new [Artist] to bind.
     * @param listener A [ClickableListListener] to bind interactions to.
     */
    fun bind(artist: Artist, listener: ClickableListListener<Artist>) {
        listener.bind(artist, this)
        binding.pickerImage.bind(artist)
        binding.pickerName.text = artist.resolveName(binding.context)
    }

    companion object {
        /**
         * Create a new instance.
         * @param parent The parent to inflate this instance from.
         * @return A new instance.
         */
        fun from(parent: View) =
            ArtistChoiceViewHolder(ItemPickerChoiceBinding.inflate(parent.context.inflater))
    }
}
