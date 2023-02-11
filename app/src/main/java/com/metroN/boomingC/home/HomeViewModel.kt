package com.metroN.boomingC.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.metroN.boomingC.home.tabs.Tab
import com.metroN.boomingC.list.Sort
import com.metroN.boomingC.music.*
import com.metroN.boomingC.music.library.Library
import com.metroN.boomingC.playback.PlaybackSettings
import com.metroN.boomingC.util.logD

/**
 * The ViewModel for managing the tab data and lists of the home view.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val homeSettings: HomeSettings,
    private val playbackSettings: PlaybackSettings,
    private val musicRepository: MusicRepository,
    private val musicSettings: MusicSettings
) : ViewModel(), MusicRepository.Listener, HomeSettings.Listener {
    private val _songsList = MutableStateFlow(listOf<Song>())
    /** A list of [Song]s, sorted by the preferred [Sort], to be shown in the home view. */
    val songsList: StateFlow<List<Song>>
        get() = _songsList

    private val _albumsLists = MutableStateFlow(listOf<Album>())
    /** A list of [Album]s, sorted by the preferred [Sort], to be shown in the home view. */
    val albumsList: StateFlow<List<Album>>
        get() = _albumsLists

    private val _artistsList = MutableStateFlow(listOf<Artist>())
    /**
     * A list of [Artist]s, sorted by the preferred [Sort], to be shown in the home view. Note that
     * if "Hide collaborators" is on, this list will not include [Artist]s where
     * [Artist.isCollaborator] is true.
     */
    val artistsList: MutableStateFlow<List<Artist>>
        get() = _artistsList

    private val _genresList = MutableStateFlow(listOf<Genre>())
    /** A list of [Genre]s, sorted by the preferred [Sort], to be shown in the home view. */
    val genresList: StateFlow<List<Genre>>
        get() = _genresList

    /** The [MusicMode] to use when playing a [Song] from the UI. */
    val playbackMode: MusicMode
        get() = playbackSettings.inListPlaybackMode

    /**
     * A list of [MusicMode] corresponding to the current [Tab] configuration, excluding invisible
     * [Tab]s.
     */
    var currentTabModes = makeTabModes()
        private set

    private val _currentTabMode = MutableStateFlow(currentTabModes[0])
    /** The [MusicMode] of the currently shown [Tab]. */
    val currentTabMode: StateFlow<MusicMode> = _currentTabMode

    private val _shouldRecreate = MutableStateFlow(false)
    /**
     * A marker to re-create all library tabs, usually initiated by a settings change. When this
     * flag is true, all tabs (and their respective ViewPager2 fragments) will be re-created from
     * scratch.
     */
    val shouldRecreate: StateFlow<Boolean> = _shouldRecreate

    private val _isFastScrolling = MutableStateFlow(false)
    /** A marker for whether the user is fast-scrolling in the home view or not. */
    val isFastScrolling: StateFlow<Boolean> = _isFastScrolling

    init {
        musicRepository.addListener(this)
        homeSettings.registerListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        musicRepository.removeListener(this)
        homeSettings.unregisterListener(this)
    }

    override fun onLibraryChanged(library: Library?) {
        if (library != null) {
            logD("Library changed, refreshing library")
            // Get the each list of items in the library to use as our list data.
            // Applying the preferred sorting to them.
            _songsList.value = musicSettings.songSort.songs(library.songs)
            _albumsLists.value = musicSettings.albumSort.albums(library.albums)
            _artistsList.value =
                musicSettings.artistSort.artists(
                    if (homeSettings.shouldHideCollaborators) {
                        // Hide Collaborators is enabled, filter out collaborators.
                        library.artists.filter { !it.isCollaborator }
                    } else {
                        library.artists
                    })
            _genresList.value = musicSettings.genreSort.genres(library.genres)
        }
    }

    override fun onTabsChanged() {
        // Tabs changed, update  the current tabs and set up a re-create event.
        currentTabModes = makeTabModes()
        _shouldRecreate.value = true
    }

    override fun onHideCollaboratorsChanged() {
        // Changes in the hide collaborator setting will change the artist contents
        // of the library, consider it a library update.
        onLibraryChanged(musicRepository.library)
    }

    /**
     * Get the preferred [Sort] for a given [Tab].
     * @param tabMode The [MusicMode] of the [Tab] desired.
     * @return The [Sort] preferred for that [Tab]
     */
    fun getSortForTab(tabMode: MusicMode) =
        when (tabMode) {
            MusicMode.SONGS -> musicSettings.songSort
            MusicMode.ALBUMS -> musicSettings.albumSort
            MusicMode.ARTISTS -> musicSettings.artistSort
            MusicMode.GENRES -> musicSettings.genreSort
        }

    /**
     * Update the preferred [Sort] for the current [Tab]. Will update corresponding list.
     * @param sort The new [Sort] to apply. Assumed to be an allowed sort for the current [Tab].
     */
    fun setSortForCurrentTab(sort: Sort) {
        logD("Updating ${_currentTabMode.value} sort to $sort")
        // Can simply re-sort the current list of items without having to access the library.
        when (_currentTabMode.value) {
            MusicMode.SONGS -> {
                musicSettings.songSort = sort
                _songsList.value = sort.songs(_songsList.value)
            }
            MusicMode.ALBUMS -> {
                musicSettings.albumSort = sort
                _albumsLists.value = sort.albums(_albumsLists.value)
            }
            MusicMode.ARTISTS -> {
                musicSettings.artistSort = sort
                _artistsList.value = sort.artists(_artistsList.value)
            }
            MusicMode.GENRES -> {
                musicSettings.genreSort = sort
                _genresList.value = sort.genres(_genresList.value)
            }
        }
    }

    /**
     * Update [currentTabMode] to reflect a new ViewPager2 position
     * @param pagerPos The new position of the ViewPager2 instance.
     */
    fun synchronizeTabPosition(pagerPos: Int) {
        logD("Updating current tab to ${currentTabModes[pagerPos]}")
        _currentTabMode.value = currentTabModes[pagerPos]
    }

    /**
     * Mark the recreation process as complete.
     * @see shouldRecreate
     */
    fun finishRecreate() {
        _shouldRecreate.value = false
    }

    /**
     * Update whether the user is fast scrolling or not in the home view.
     * @param isFastScrolling true if the user is currently fast scrolling, false otherwise.
     */
    fun setFastScrolling(isFastScrolling: Boolean) {
        logD("Updating fast scrolling state: $isFastScrolling")
        _isFastScrolling.value = isFastScrolling
    }

    /**
     * Create a list of [MusicMode]s representing a simpler version of the [Tab] configuration.
     * @return A list of the [MusicMode]s for each visible [Tab] in the configuration, ordered in
     * the same way as the configuration.
     */
    private fun makeTabModes() =
        homeSettings.homeTabs.filterIsInstance<Tab.Visible>().map { it.mode }
}
