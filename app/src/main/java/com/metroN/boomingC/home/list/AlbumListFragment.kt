package com.metroN.boomingC.home.list

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import java.util.Formatter
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.FragmentHomeListBinding
import com.metroN.boomingC.home.HomeViewModel
import com.metroN.boomingC.home.fastscroll.FastScrollRecyclerView
import com.metroN.boomingC.list.*
import com.metroN.boomingC.list.ListFragment
import com.metroN.boomingC.list.Sort
import com.metroN.boomingC.list.adapter.BasicListInstructions
import com.metroN.boomingC.list.adapter.ListDiffer
import com.metroN.boomingC.list.adapter.SelectionIndicatorAdapter
import com.metroN.boomingC.list.recycler.AlbumViewHolder
import com.metroN.boomingC.list.selection.SelectionViewModel
import com.metroN.boomingC.music.*
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.playback.secsToMs
import com.metroN.boomingC.ui.NavigationViewModel
import com.metroN.boomingC.util.collectImmediately

/**
 * A [ListFragment] that shows a list of [Album]s.
 */
@AndroidEntryPoint
class AlbumListFragment :
    ListFragment<Album, FragmentHomeListBinding>(),
    FastScrollRecyclerView.Listener,
    FastScrollRecyclerView.PopupProvider {
    private val homeModel: HomeViewModel by activityViewModels()
    override val navModel: NavigationViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    override val selectionModel: SelectionViewModel by activityViewModels()
    private val albumAdapter = AlbumAdapter(this)
    // Save memory by re-using the same formatter and string builder when creating popup text
    private val formatterSb = StringBuilder(64)
    private val formatter = Formatter(formatterSb)

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentHomeListBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentHomeListBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.homeRecycler.apply {
            id = R.id.home_album_recycler
            adapter = albumAdapter
            popupProvider = this@AlbumListFragment
            listener = this@AlbumListFragment
        }

        collectImmediately(homeModel.albumsList, ::updateList)
        collectImmediately(selectionModel.selected, ::updateSelection)
        collectImmediately(playbackModel.parent, playbackModel.isPlaying, ::updatePlayback)
    }

    override fun onDestroyBinding(binding: FragmentHomeListBinding) {
        super.onDestroyBinding(binding)
        binding.homeRecycler.apply {
            adapter = null
            popupProvider = null
            listener = null
        }
    }

    override fun getPopup(pos: Int): String? {
        val album = homeModel.albumsList.value[pos]
        // Change how we display the popup depending on the current sort mode.
        return when (homeModel.getSortForTab(MusicMode.ALBUMS).mode) {
            // By Name -> Use Name
            is Sort.Mode.ByName -> album.collationKey?.run { sourceString.first().uppercase() }

            // By Artist -> Use name of first artist
            is Sort.Mode.ByArtist ->
                album.artists[0].collationKey?.run { sourceString.first().uppercase() }

            // Date -> Use minimum date (Maximum dates are not sorted by, so showing them is odd)
            is Sort.Mode.ByDate -> album.dates?.run { min.resolveDate(requireContext()) }

            // Duration -> Use formatted duration
            is Sort.Mode.ByDuration -> album.durationMs.formatDurationMs(false)

            // Count -> Use song count
            is Sort.Mode.ByCount -> album.songs.size.toString()

            // Last added -> Format as date
            is Sort.Mode.ByDateAdded -> {
                val dateAddedMillis = album.dateAdded.secsToMs()
                formatterSb.setLength(0)
                DateUtils.formatDateRange(
                        context,
                        formatter,
                        dateAddedMillis,
                        dateAddedMillis,
                        DateUtils.FORMAT_ABBREV_ALL)
                    .toString()
            }

            // Unsupported sort, error gracefully
            else -> null
        }
    }

    override fun onFastScrollingChanged(isFastScrolling: Boolean) {
        homeModel.setFastScrolling(isFastScrolling)
    }

    override fun onRealClick(item: Album) {
        navModel.exploreNavigateTo(item)
    }

    override fun onOpenMenu(item: Album, anchor: View) {
        openMusicMenu(anchor, R.menu.menu_album_actions, item)
    }

    private fun updateList(albums: List<Album>) {
        albumAdapter.submitList(albums, BasicListInstructions.REPLACE)
    }

    private fun updateSelection(selection: List<Music>) {
        albumAdapter.setSelected(selection.filterIsInstanceTo(mutableSetOf()))
    }

    private fun updatePlayback(parent: MusicParent?, isPlaying: Boolean) {
        // If an album is playing, highlight it within this adapter.
        albumAdapter.setPlaying(parent as? Album, isPlaying)
    }

    /**
     * A [SelectionIndicatorAdapter] that shows a list of [Album]s using [AlbumViewHolder].
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    private class AlbumAdapter(private val listener: SelectableListListener<Album>) :
        SelectionIndicatorAdapter<Album, BasicListInstructions, AlbumViewHolder>(
            ListDiffer.Blocking(AlbumViewHolder.DIFF_CALLBACK)) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AlbumViewHolder.from(parent)

        override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
            holder.bind(getItem(position), listener)
        }
    }
}
