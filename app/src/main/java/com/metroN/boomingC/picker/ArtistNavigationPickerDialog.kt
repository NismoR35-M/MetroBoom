package com.metroN.boomingC.picker

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.databinding.DialogMusicPickerBinding
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.ui.NavigationViewModel

/**
 * An [ArtistPickerDialog] intended for when [Artist] navigation is ambiguous.
 */
@AndroidEntryPoint
class ArtistNavigationPickerDialog : ArtistPickerDialog() {
    private val navModel: NavigationViewModel by activityViewModels()
    // Information about what Song to show choices for is initially within the navigation arguments
    // as UIDs, as that is the only safe way to parcel a Song.
    private val args: ArtistNavigationPickerDialogArgs by navArgs()

    override fun onBindingCreated(binding: DialogMusicPickerBinding, savedInstanceState: Bundle?) {
        pickerModel.setItemUid(args.itemUid)
        super.onBindingCreated(binding, savedInstanceState)
    }

    override fun onClick(item: Artist, viewHolder: RecyclerView.ViewHolder) {
        super.onClick(item, viewHolder)
        // User made a choice, navigate to it.
        navModel.exploreNavigateTo(item)
    }
}
