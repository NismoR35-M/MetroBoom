package com.metroN.boomingC.list.selection

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.metroN.boomingC.music.*
import com.metroN.boomingC.music.library.Library

/**
 * A [ViewModel] that manages the current selection.
 */
@HiltViewModel
class SelectionViewModel @Inject constructor(private val musicRepository: MusicRepository) :
    ViewModel(), MusicRepository.Listener {
    private val _selected = MutableStateFlow(listOf<Music>())
    /** the currently selected items. These are ordered in earliest selected and latest selected. */
    val selected: StateFlow<List<Music>>
        get() = _selected

    init {
        musicRepository.addListener(this)
    }

    override fun onLibraryChanged(library: Library?) {
        if (library == null) {
            return
        }

        // Sanitize the selection to remove items that no longer exist and thus
        // won't appear in any list.
        _selected.value =
            _selected.value.mapNotNull {
                when (it) {
                    is Song -> library.sanitize(it)
                    is Album -> library.sanitize(it)
                    is Artist -> library.sanitize(it)
                    is Genre -> library.sanitize(it)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        musicRepository.removeListener(this)
    }

    /**
     * Select a new [Music] item. If this item is already within the selected items, the item will
     * be removed. Otherwise, it will be added.
     * @param music The [Music] item to select.
     */
    fun select(music: Music) {
        val selected = _selected.value.toMutableList()
        if (!selected.remove(music)) {
            selected.add(music)
        }
        _selected.value = selected
    }

    /**
     * Consume the current selection. This will clear any items that were selected prior.
     * @return The list of selected items before it was cleared.
     */
    fun consume() = _selected.value.also { _selected.value = listOf() }
}
