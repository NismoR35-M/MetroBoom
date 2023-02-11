package com.metroN.boomingC.picker

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.metroN.boomingC.music.*
import com.metroN.boomingC.music.library.Library
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * a [ViewModel] that manages the current music picker state. Make it so that the dialogs just
 * contain the music themselves and then exit if the library changes.*/
@HiltViewModel
class PickerViewModel @Inject constructor(private val musicRepository: MusicRepository) :
    ViewModel(), MusicRepository.Listener {

    private val _currentItem = MutableStateFlow<Music?>(null)
    /** The current item whose artists should be shown in the picker. Null if there is no item. */
    val currentItem: StateFlow<Music?>
        get() = _currentItem

    private val _artistChoices = MutableStateFlow<List<Artist>>(listOf())
    /** The current [Artist] choices. Empty if no item is shown in the picker. */
    val artistChoices: StateFlow<List<Artist>>
        get() = _artistChoices

    private val _genreChoices = MutableStateFlow<List<Genre>>(listOf())
    /** The current [Genre] choices. Empty if no item is shown in the picker. */
    val genreChoices: StateFlow<List<Genre>>
        get() = _genreChoices

    override fun onCleared() {
        musicRepository.removeListener(this)
    }

    override fun onLibraryChanged(library: Library?) {
        if (library != null) {
            refreshChoices()
        }
    }

    /**
     * Set a new [currentItem] from it's [Music.UID].
     * @param uid The [Music.UID] of the [Song] to update to.
     */
    fun setItemUid(uid: Music.UID) {
        val library = unlikelyToBeNull(musicRepository.library)
        _currentItem.value = library.find(uid)
        refreshChoices()
    }

    private fun refreshChoices() {
        when (val item = _currentItem.value) {
            is Song -> {
                _artistChoices.value = item.artists
                _genreChoices.value = item.genres
            }
            is Album -> _artistChoices.value = item.artists
            else -> {}
        }
    }
}
