package com.metroN.boomingC.home.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
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
import com.metroN.boomingC.list.recycler.GenreViewHolder
import com.metroN.boomingC.list.selection.SelectionViewModel
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.playback.formatDurationMs
import com.metroN.boomingC.ui.NavigationViewModel
import com.metroN.boomingC.util.collectImmediately

/**
 * A [ListFragment] that shows a list of [Genre]s.
 */
@AndroidEntryPoint
class GenreListFragment :
    ListFragment<Genre, FragmentHomeListBinding>(),
    FastScrollRecyclerView.PopupProvider,
    FastScrollRecyclerView.Listener {
    private val homeModel: HomeViewModel by activityViewModels()
    override val navModel: NavigationViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    override val selectionModel: SelectionViewModel by activityViewModels()
    private val genreAdapter = GenreAdapter(this)

    override fun onCreateBinding(inflater: LayoutInflater) =
        FragmentHomeListBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentHomeListBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        binding.homeRecycler.apply {
            id = R.id.home_genre_recycler
            adapter = genreAdapter
            popupProvider = this@GenreListFragment
            listener = this@GenreListFragment
        }

        collectImmediately(homeModel.genresList, ::updateList)
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
        val genre = homeModel.genresList.value[pos]
        // Change how we display the popup depending on the current sort mode.
        return when (homeModel.getSortForTab(MusicMode.GENRES).mode) {
            // By Name -> Use Name
            is Sort.Mode.ByName -> genre.collationKey?.run { sourceString.first().uppercase() }

            // Duration -> Use formatted duration
            is Sort.Mode.ByDuration -> genre.durationMs.formatDurationMs(false)

            // Count -> Use song count
            is Sort.Mode.ByCount -> genre.songs.size.toString()

            // Unsupported sort, error gracefully
            else -> null
        }
    }

    override fun onFastScrollingChanged(isFastScrolling: Boolean) {
        homeModel.setFastScrolling(isFastScrolling)
    }

    override fun onRealClick(item: Genre) {
        navModel.exploreNavigateTo(item)
    }

    override fun onOpenMenu(item: Genre, anchor: View) {
        openMusicMenu(anchor, R.menu.menu_artist_actions, item)
    }

    private fun updateList(artists: List<Genre>) {
        genreAdapter.submitList(artists, BasicListInstructions.REPLACE)
    }

    private fun updateSelection(selection: List<Music>) {
        genreAdapter.setSelected(selection.filterIsInstanceTo(mutableSetOf()))
    }

    private fun updatePlayback(parent: MusicParent?, isPlaying: Boolean) {
        // If a genre is playing, highlight it within this adapter.
        genreAdapter.setPlaying(parent as? Genre, isPlaying)
    }

    /**
     * A [SelectionIndicatorAdapter] that shows a list of [Genre]s using [GenreViewHolder].
     * @param listener An [SelectableListListener] to bind interactions to.
     */
    private class GenreAdapter(private val listener: SelectableListListener<Genre>) :
        SelectionIndicatorAdapter<Genre, BasicListInstructions, GenreViewHolder>(
            ListDiffer.Blocking(GenreViewHolder.DIFF_CALLBACK)) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            GenreViewHolder.from(parent)

        override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
            holder.bind(getItem(position), listener)
        }
    }
}
