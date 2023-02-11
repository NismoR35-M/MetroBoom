package com.metroN.boomingC.playback.persist

import android.content.Context
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.music.library.Library
import com.metroN.boomingC.playback.queue.Queue
import com.metroN.boomingC.playback.state.PlaybackStateManager
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.logE

/**
 * Manages the persisted playback state in a structured manner.
 */
interface PersistenceRepository {
    /**
     * Read the previously persisted [PlaybackStateManager.SavedState].
     * @param library The [Library] required to de-serialize the [PlaybackStateManager.SavedState].
     */
    suspend fun readState(library: Library): PlaybackStateManager.SavedState?

    /**
     * Persist a new [PlaybackStateManager.SavedState].
     * @param state The [PlaybackStateManager.SavedState] to persist.
     */
    suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): PersistenceRepository = RealPersistenceRepository(context)
    }
}

private class RealPersistenceRepository(private val context: Context) : PersistenceRepository {
    private val database: PersistenceDatabase by lazy { PersistenceDatabase.getInstance(context) }
    private val playbackStateDao: PlaybackStateDao by lazy { database.playbackStateDao() }
    private val queueDao: QueueDao by lazy { database.queueDao() }

    override suspend fun readState(library: Library): PlaybackStateManager.SavedState? {
        val playbackState: PlaybackState
        val heap: List<QueueHeapItem>
        val mapping: List<QueueMappingItem>
        try {
            playbackState = playbackStateDao.getState() ?: return null
            heap = queueDao.getHeap()
            mapping = queueDao.getMapping()
        } catch (e: Exception) {
            logE("Unable to load playback state data")
            logE(e.stackTraceToString())
            return null
        }

        val orderedMapping = mutableListOf<Int>()
        val shuffledMapping = mutableListOf<Int>()
        for (entry in mapping) {
            orderedMapping.add(entry.orderedIndex)
            shuffledMapping.add(entry.shuffledIndex)
        }

        val parent = playbackState.parentUid?.let { library.find<MusicParent>(it) }
        logD("Read playback state")

        return PlaybackStateManager.SavedState(
            parent = parent,
            queueState =
                Queue.SavedState(
                    heap.map { library.find(it.uid) },
                    orderedMapping,
                    shuffledMapping,
                    playbackState.index,
                    playbackState.songUid),
            positionMs = playbackState.positionMs,
            repeatMode = playbackState.repeatMode)
    }

    override suspend fun saveState(state: PlaybackStateManager.SavedState?): Boolean {
        // Only bother saving a state if a song is actively playing from one.
        // This is not the case with a null state.
        try {
            playbackStateDao.nukeState()
            queueDao.nukeHeap()
            queueDao.nukeMapping()
        } catch (e: Exception) {
            logE("Unable to clear previous state")
            logE(e.stackTraceToString())
            return false
        }
        logD("Cleared state")
        if (state != null) {
            // Transform saved state into raw state, which can then be written to the database.
            val playbackState =
                PlaybackState(
                    id = 0,
                    index = state.queueState.index,
                    positionMs = state.positionMs,
                    repeatMode = state.repeatMode,
                    songUid = state.queueState.songUid,
                    parentUid = state.parent?.uid)

            // Convert the remaining queue information do their database-specific counterparts.
            val heap =
                state.queueState.heap.mapIndexed { i, song ->
                    QueueHeapItem(i, requireNotNull(song).uid)
                }
            val mapping =
                state.queueState.orderedMapping.zip(state.queueState.shuffledMapping).mapIndexed {
                    i,
                    pair ->
                    QueueMappingItem(i, pair.first, pair.second)
                }
            try {
                playbackStateDao.insertState(playbackState)
                queueDao.insertHeap(heap)
                queueDao.insertMapping(mapping)
            } catch (e: Exception) {
                logE("Unable to write new state")
                logE(e.stackTraceToString())
                return false
            }
            logD("Wrote state")
        }
        return true
    }
}
