package com.metroN.boomingC.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.FragmentDetailBinding
import com.metroN.boomingC.detail.recycler.DetailAdapter
import com.metroN.boomingC.detail.recycler.GenreDetailAdapter
import com.metroN.boomingC.list.Item
import com.metroN.boomingC.list.ListFragment
import com.metroN.boomingC.list.Sort
import com.metroN.boomingC.list.adapter.BasicListInstructions
import com.metroN.boomingC.list.selection.SelectionViewModel
import com.metroN.boomingC.music.Album
import com.metroN.boomingC.music.Artist
import com.metroN.boomingC.music.Genre
import com.metroN.boomingC.music.Music
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.ui.NavigationViewModel
import com.metroN.boomingC.util.collect
import com.metroN.boomingC.util.collectImmediately
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.showToast
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * A [ListFragment] that shows information for a particular [Genre].
 */
@AndroidEntryPoint
class GenreDetailFragment :
    ListFragment<Music, FragmentDetailBinding>(), DetailAdapter.Listener<Music> {
    private val detailModel: DetailViewModel by activityViewModels()
    override val navModel: NavigationViewModel by activityViewModels()
    override val playbackModel: PlaybackViewModel by activityViewModels()
    override val selectionModel: SelectionViewModel by activityViewModels()
    // Information about what genre to display is initially within the navigation arguments
    // as a UID, as that is the only safe way to parcel an genre.
    private val args: GenreDetailFragmentArgs by navArgs()
    private val detailAdapter = GenreDetailAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateBinding(inflater: LayoutInflater) = FragmentDetailBinding.inflate(inflater)

    override fun getSelectionToolbar(binding: FragmentDetailBinding) =
        binding.detailSelectionToolbar

    override fun onBindingCreated(binding: FragmentDetailBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- UI SETUP ---
        binding.detailToolbar.apply {
            inflateMenu(R.menu.menu_genre_artist_detail)
            setNavigationOnClickListener { findNavController().navigateUp() }
            setOnMenuItemClickListener(this@GenreDetailFragment)
        }

        binding.detailRecycler.adapter = detailAdapter

        // --- VIEWMODEL SETUP ---
        // DetailViewModel handles most initialization from the navigation argument.
        detailModel.setGenreUid(args.genreUid)
        collectImmediately(detailModel.currentGenre, ::updateItem)
        collectImmediately(detailModel.genreList, ::updateList)
        collectImmediately(
            playbackModel.song, playbackModel.parent, playbackModel.isPlaying, ::updatePlayback)
        collect(navModel.exploreNavigationItem, ::handleNavigation)
        collectImmediately(selectionModel.selected, ::updateSelection)
    }

    override fun onDestroyBinding(binding: FragmentDetailBinding) {
        super.onDestroyBinding(binding)
        binding.detailToolbar.setOnMenuItemClickListener(null)
        binding.detailRecycler.adapter = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (super.onMenuItemClick(item)) {
            return true
        }

        val currentGenre = unlikelyToBeNull(detailModel.currentGenre.value)
        return when (item.itemId) {
            R.id.action_play_next -> {
                playbackModel.playNext(currentGenre)
                requireContext().showToast(R.string.lng_queue_added)
                true
            }
            R.id.action_queue_add -> {
                playbackModel.addToQueue(currentGenre)
                requireContext().showToast(R.string.lng_queue_added)
                true
            }
            else -> false
        }
    }

    override fun onRealClick(item: Music) {
        when (item) {
            is Artist -> navModel.exploreNavigateTo(item)
            is Song -> {
                val playbackMode = detailModel.playbackMode
                if (playbackMode != null) {
                    playbackModel.playFrom(item, playbackMode)
                } else {
                    // When configured to play from the selected item, we already have an Genre
                    // to play from.
                    playbackModel.playFromGenre(
                        item, unlikelyToBeNull(detailModel.currentGenre.value))
                }
            }
            else -> error("Unexpected datatype: ${item::class.simpleName}")
        }
    }

    override fun onOpenMenu(item: Music, anchor: View) {
        when (item) {
            is Artist -> openMusicMenu(anchor, R.menu.menu_artist_actions, item)
            is Song -> openMusicMenu(anchor, R.menu.menu_song_actions, item)
            else -> error("Unexpected datatype: ${item::class.simpleName}")
        }
    }

    override fun onPlay() {
        playbackModel.play(unlikelyToBeNull(detailModel.currentGenre.value))
    }

    override fun onShuffle() {
        playbackModel.shuffle(unlikelyToBeNull(detailModel.currentGenre.value))
    }

    override fun onOpenSortMenu(anchor: View) {
        openMenu(anchor, R.menu.menu_genre_sort) {
            val sort = detailModel.genreSongSort
            unlikelyToBeNull(menu.findItem(sort.mode.itemId)).isChecked = true
            val directionItemId =
                when (sort.direction) {
                    Sort.Direction.ASCENDING -> R.id.option_sort_asc
                    Sort.Direction.DESCENDING -> R.id.option_sort_dec
                }
            unlikelyToBeNull(menu.findItem(directionItemId)).isChecked = true
            setOnMenuItemClickListener { item ->
                item.isChecked = !item.isChecked
                detailModel.genreSongSort =
                    when (item.itemId) {
                        R.id.option_sort_asc -> sort.withDirection(Sort.Direction.ASCENDING)
                        R.id.option_sort_dec -> sort.withDirection(Sort.Direction.DESCENDING)
                        else -> sort.withMode(unlikelyToBeNull(Sort.Mode.fromItemId(item.itemId)))
                    }
                true
            }
        }
    }

    private fun updateItem(genre: Genre?) {
        if (genre == null) {
            // Genre we were showing no longer exists.
            findNavController().navigateUp()
            return
        }

        requireBinding().detailToolbar.title = genre.resolveName(requireContext())
    }

    private fun updatePlayback(song: Song?, parent: MusicParent?, isPlaying: Boolean) {
        var playingMusic: Music? = null
        if (parent is Artist) {
            playingMusic = parent
        }
        // Prefer songs that might be playing from this genre.
        if (parent is Genre && parent.uid == unlikelyToBeNull(detailModel.currentGenre.value).uid) {
            playingMusic = song
        }
        detailAdapter.setPlaying(playingMusic, isPlaying)
    }

    private fun handleNavigation(item: Music?) {
        when (item) {
            is Song -> {
                logD("Navigating to another song")
                findNavController()
                    .navigate(GenreDetailFragmentDirections.actionShowAlbum(item.album.uid))
            }
            is Album -> {
                logD("Navigating to another album")
                findNavController()
                    .navigate(GenreDetailFragmentDirections.actionShowAlbum(item.uid))
            }
            is Artist -> {
                logD("Navigating to another artist")
                findNavController()
                    .navigate(GenreDetailFragmentDirections.actionShowArtist(item.uid))
            }
            is Genre -> {
                navModel.finishExploreNavigation()
            }
            null -> {}
        }
    }

    private fun updateList(items: List<Item>) {
        detailAdapter.submitList(items, BasicListInstructions.DIFF)
    }

    private fun updateSelection(selected: List<Music>) {
        detailAdapter.setSelected(selected.toSet())
        requireBinding().detailSelectionToolbar.updateSelectionAmount(selected.size)
    }
}
