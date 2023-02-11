package com.metroN.boomingC.picker

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogMusicPickerBinding
import com.metroN.boomingC.list.ClickableListListener
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.collectImmediately
import com.metroN.boomingC.util.requireIs
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * A picker [ViewBindingDialogFragment] intended for when [Genre] playback is ambiguous.
 */
@AndroidEntryPoint
class GenrePlaybackPickerDialog :
    ViewBindingDialogFragment<DialogMusicPickerBinding>(), ClickableListListener<Genre> {
    private val pickerModel: PickerViewModel by viewModels()
    private val playbackModel: PlaybackViewModel by activityViewModels()
    // Information about what Song to show choices for is initially within the navigation arguments
    // as UIDs, as that is the only safe way to parcel a Song.
    private val args: GenrePlaybackPickerDialogArgs by navArgs()
    // Okay to leak this since the Listener will not be called until after initialization.
    private val genreAdapter = GenreChoiceAdapter(@Suppress("LeakingThis") this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogMusicPickerBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.lbl_genres).setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogMusicPickerBinding, savedInstanceState: Bundle?) {
        binding.pickerRecycler.adapter = genreAdapter

        pickerModel.setItemUid(args.itemUid)
        collectImmediately(pickerModel.genreChoices) { genres ->
            if (genres.isNotEmpty()) {
                // Make sure the genre choices align with any changes in the music library.
                genreAdapter.submitList(genres)
            } else {
                // Not showing any choices, navigate up.
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyBinding(binding: DialogMusicPickerBinding) {
        binding.pickerRecycler.adapter = null
    }

    override fun onClick(item: Genre, viewHolder: RecyclerView.ViewHolder) {
        // User made a choice, play the given song from that genre.
        val song = requireIs<Song>(unlikelyToBeNull(pickerModel.currentItem.value))
        playbackModel.playFromGenre(song, item)
    }
}
