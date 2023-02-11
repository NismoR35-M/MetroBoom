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
import com.metroN.boomingC.list.recycler.SongViewHolder
import com.metroN.boomingC.list.selection.SelectionViewModel
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.playback.secsToMs
import com.metroN.boomingC.ui.NavigationViewModel
import com.metroN.boomingC.util.collectImmediately

/**
 * A [ListFragment] that shows a list of [Song]s.
 */
@AndroidEntryPoint
class SongListFragment :
    ListFragment<Song, FragmentHomeListBinding>(),
    FastScrollRecyclerView.PopupProvider,
    FastScrollRecyclerView.Listener {
    private val homeModel: HomeViewModel by activityViewModels()
    override val navModel: NavigationViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    override val selectionModel: SelectionViewModel by activityViewModels()
    private val songAdapter = SongAdapter(this)
    // Save memory by re-using the same formatter and string builder when creating popup text
    private val formatterSb = StringBuilder(64)
    private val formatter = Formatter(formatterSb)

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentHomeListBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentHomeListBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.homeRecycler.apply {
            id = R.id.home_song_recycler
            adapter = songAdapter
            popupProvider = this@SongListFragment
            listener = this@SongListFragment
        }

        collectImmediately(homeModel.songsList, ::updateList)
        collectImmediately(selectionModel.selected, ::updateSelection)
        collectImmediately(
            playbackModel.song, playbackModel.parent, playbackModel.isPlaying, ::updatePlayback)
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
        val song = homeModel.songsList.value[pos]
        // Change how we display the popup depending on the current sort mode.
        // Note: We don't use the more correct individual artist name here, as sorts are largely
        // based off the names of the parent objects and not the child objects.
        return when (homeModel.getSortForTab(MusicMode.SONGS).mode) {
            // Name -> Use name
            is Sort.Mode.ByName -> song.collationKey?.run { sourceString.first().uppercase() }

            // Artist -> Use name of first artist
            is Sort.Mode.ByArtist ->
                song.album.artists[0].collationKey?.run { sourceString.first().uppercase() }

            // Album -> Use Album Name
            is Sort.Mode.ByAlbum ->
                song.album.collationKey?.run { sourceString.first().uppercase() }

            // Year -> Use Full Year
            is Sort.Mode.ByDate -> song.album.dates?.resolveDate(requireContext())

            // Duration -> Use formatted duration
            is Sort.Mode.ByDuration -> song.durationMs.formatDurationMs(false)

            // Last added -> Format as date
            is Sort.Mode.ByDateAdded -> {
                val dateAddedMillis = song.dateAdded.secsToMs()
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

    override fun onRealClick(item: Song) {
        playbackModel.playFrom(item, homeModel.playbackMode)
    }

    override fun onOpenMenu(item: Song, anchor: View) {
        openMusicMenu(anchor, R.menu.menu_song_actions, item)
    }

    private fun updateList(songs: List<Song>) {
        songAdapter.submitList(songs, BasicListInstructions.REPLACE)
    }

    private fun updateSelection(selection: List<Music>) {
        songAdapter.setSelected(selection.filterIsInstanceTo(mutableSetOf()))
    }

    private fun updatePlayback(song: Song?, parent: MusicParent?, isPlaying: Boolean) {
        if (parent == null) {
            songAdapter.setPlaying(song, isPlaying)
        } else {
            // Ignore playback that is not from all songs
            songAdapter.setPlaying(null, isPlaying)
        }
    }

    /**
     * A [SelectionIndicatorAdapter] that shows a list of [Song]s using [SongViewHolder].
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    private class SongAdapter(private val listener: SelectableListListener<Song>) :
        SelectionIndicatorAdapter<Song, BasicListInstructions, SongViewHolder>(
            ListDiffer.Blocking(SongViewHolder.DIFF_CALLBACK)) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SongViewHolder.from(parent)

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            holder.bind(getItem(position), listener)
        }
    }
}
