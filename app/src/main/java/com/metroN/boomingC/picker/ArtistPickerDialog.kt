package com.metroN.boomingC.picker

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogMusicPickerBinding
import com.metroN.boomingC.list.ClickableListListener
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.collectImmediately

/**
 * The base class for dialogs that implements common behavior across all [Artist] pickers. These are
 * shown whenever what to do with an item's [Artist] is ambiguous, as there are multiple [Artist]'s
 * to choose from.
 */
@AndroidEntryPoint
abstract class ArtistPickerDialog :
    ViewBindingDialogFragment<DialogMusicPickerBinding>(), ClickableListListener<Artist> {
    protected val pickerModel: PickerViewModel by viewModels()
    // Okay to leak this since the Listener will not be called until after initialization.
    private val artistAdapter = ArtistChoiceAdapter(@Suppress("LeakingThis") this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogMusicPickerBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder.setTitle(R.string.lbl_artists).setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogMusicPickerBinding, savedInstanceState: Bundle?) {
        binding.pickerRecycler.adapter = artistAdapter

        collectImmediately(pickerModel.artistChoices) { artists ->
            if (artists.isNotEmpty()) {
                // Make sure the artist choices align with any changes in the music library.
                artistAdapter.submitList(artists)
            } else {
                // Not showing any choices, navigate up.
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyBinding(binding: DialogMusicPickerBinding) {
        binding.pickerRecycler.adapter = null
    }

    override fun onClick(item: Artist, viewHolder: RecyclerView.ViewHolder) {
        findNavController().navigateUp()
    }
}
