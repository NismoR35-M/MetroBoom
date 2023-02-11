package com.metroN.boomingC.playback.queue

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.metroN.boomingC.list.adapter.BasicListInstructions
import com.metroN.boomingC.music.MusicParent
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.state.PlaybackStateManager

/**
 * A [ViewModel] that manages the current queue state and allows navigation through the queue.
 */
@HiltViewModel
class QueueViewModel @Inject constructor(private val playbackManager: PlaybackStateManager) :
    ViewModel(), PlaybackStateManager.Listener {

    private val _queue = MutableStateFlow(listOf<Song>())
    /** The current queue. */
    val queue: StateFlow<List<Song>> = _queue

    private val _index = MutableStateFlow(playbackManager.queue.index)
    /** The index of the currently playing song in the queue. */
    val index: StateFlow<Int>
        get() = _index

    /** Specifies how to update the list when the queue changes. */
    var queueListInstructions: ListInstructions? = null

    init {
        playbackManager.addListener(this)
    }

    override fun onIndexMoved(queue: Queue) {
        queueListInstructions = ListInstructions(null, queue.index)
        _index.value = queue.index
    }

    override fun onQueueChanged(queue: Queue, change: Queue.ChangeResult) {
        // Queue changed trivially due to item mo -> Diff queue, stay at current index.
        queueListInstructions = ListInstructions(BasicListInstructions.DIFF, null)
        _queue.value = queue.resolve()
        if (change != Queue.ChangeResult.MAPPING) {
            // Index changed, make sure it remains updated without actually scrolling to it.
            _index.value = queue.index
        }
    }

    override fun onQueueReordered(queue: Queue) {
        // Queue changed completely -> Replace queue, update index
        queueListInstructions = ListInstructions(BasicListInstructions.REPLACE, queue.index)
        _queue.value = queue.resolve()
        _index.value = queue.index
    }

    override fun onNewPlayback(queue: Queue, parent: MusicParent?) {
        // Entirely new queue -> Replace queue, update index
        queueListInstructions = ListInstructions(BasicListInstructions.REPLACE, queue.index)
        _queue.value = queue.resolve()
        _index.value = queue.index
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.removeListener(this)
    }

    /**
     * Start playing the the queue item at the given index.
     * @param adapterIndex The index of the queue item to play. Does nothing if the index is out of
     * range.
     */
    fun goto(adapterIndex: Int) {
        playbackManager.goto(adapterIndex)
    }

    /**
     * Remove a queue item at the given index.
     * @param adapterIndex The index of the queue item to play. Does nothing if the index is out of
     * range.
     */
    fun removeQueueDataItem(adapterIndex: Int) {
        if (adapterIndex !in queue.value.indices) {
            return
        }
        playbackManager.removeQueueItem(adapterIndex)
    }

    /**
     * Move a queue item from one index to another index.
     * @param adapterFrom The index of the queue item to move.
     * @param adapterTo The destination index for the queue item.
     * @return true if the items were moved, false otherwise.
     */
    fun moveQueueDataItems(adapterFrom: Int, adapterTo: Int): Boolean {
        if (adapterFrom !in queue.value.indices || adapterTo !in queue.value.indices) {
            return false
        }
        playbackManager.moveQueueItem(adapterFrom, adapterTo)
        return true
    }

    /** Signal that the specified [ListInstructions] in [queueListInstructions] were performed. */
    fun finishInstructions() {
        queueListInstructions = null
    }

    class ListInstructions(val update: BasicListInstructions?, val scrollTo: Int?)
}
