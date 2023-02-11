package com.metroN.boomingC.music

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.metroN.boomingC.music.system.Indexer

/**
 * A [ViewModel] providing data specific to the music loading process.
 */
@HiltViewModel
class MusicViewModel @Inject constructor(private val indexer: Indexer) :
    ViewModel(), Indexer.Listener {

    private val _indexerState = MutableStateFlow<Indexer.State?>(null)
    /** The current music loading state, or null if no loading is going on. */
    val indexerState: StateFlow<Indexer.State?> = _indexerState

    private val _statistics = MutableStateFlow<Statistics?>(null)
    /** [Statistics] about the last completed music load. */
    val statistics: StateFlow<Statistics?>
        get() = _statistics

    init {
        indexer.registerListener(this)
    }

    override fun onCleared() {
        indexer.unregisterListener(this)
    }

    override fun onIndexerStateChanged(state: Indexer.State?) {
        _indexerState.value = state
        if (state is Indexer.State.Complete) {
            // New state is a completed library, update the statistics values.
            val library = state.result.getOrNull() ?: return
            _statistics.value =
                Statistics(
                    library.songs.size,
                    library.albums.size,
                    library.artists.size,
                    library.genres.size,
                    library.songs.sumOf { it.durationMs })
        }
    }

    /** Requests that the music library should be re-loaded while leveraging the cache. */
    fun refresh() {
        indexer.requestReindex(true)
    }

    /** Requests that the music library be re-loaded without the cache. */
    fun rescan() {
        indexer.requestReindex(false)
    }

    /**
     * Non-manipulated statistics bound the last successful music load.
     * @param songs The amount of [Song]s that were loaded.
     * @param albums The amount of [Album]s that were created.
     * @param artists The amount of [Artist]s that were created.
     * @param genres The amount of [Genre]s that were created.
     * @param durationMs The total duration of all songs in the library, in milliseconds.
     */
    data class Statistics(
        val songs: Int,
        val albums: Int,
        val artists: Int,
        val genres: Int,
        val durationMs: Long
    )
}
