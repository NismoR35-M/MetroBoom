package com.metroN.boomingC.picker

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.databinding.DialogMusicPickerBinding
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.util.requireIs
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * An [ArtistPickerDialog] intended for when [Artist] playback is ambiguous.
 */
@AndroidEntryPoint
class ArtistPlaybackPickerDialog : ArtistPickerDialog() {
    private val playbackModel: PlaybackViewModel by activityViewModels()
    // Information about what Song to show choices for is initially within the navigation arguments
    // as UIDs, as that is the only safe way to parcel a Song.
    private val args: ArtistPlaybackPickerDialogArgs by navArgs()

    override fun onBindingCreated(binding: DialogMusicPickerBinding, savedInstanceState: Bundle?) {
        pickerModel.setItemUid(args.itemUid)
        super.onBindingCreated(binding, savedInstanceState)
    }

    override fun onClick(item: Artist, viewHolder: RecyclerView.ViewHolder) {
        super.onClick(item, viewHolder)
        // User made a choice, play the given song from that artist.
        val song = requireIs<Song>(unlikelyToBeNull(pickerModel.currentItem.value))
        playbackModel.playFromArtist(song, item)
    }
}
