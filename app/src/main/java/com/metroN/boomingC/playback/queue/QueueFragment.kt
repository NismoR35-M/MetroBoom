package com.metroN.boomingC.playback.queue

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.min
import com.metroN.boomingC.databinding.FragmentQueueBinding
import com.metroN.boomingC.list.EditableListListener
import com.metroN.boomingC.list.adapter.BasicListInstructions
import com.metroN.boomingC.music.Song
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.ui.ViewBindingFragment
import com.metroN.boomingC.util.collectImmediately

/**
 * A [ViewBindingFragment] that displays an editable queue.
 */
@AndroidEntryPoint
class QueueFragment : ViewBindingFragment<FragmentQueueBinding>(), EditableListListener<Song> {
    private val queueModel: QueueViewModel by activityViewModels()
    private val playbackModel: PlaybackViewModel by activityViewModels()
    private val queueAdapter = QueueAdapter(this)
    private var touchHelper: ItemTouchHelper? = null

    override fun onCreateBinding(inflater: LayoutInflater) = FragmentQueueBinding.inflate(inflater)

    override fun onBindingCreated(binding: FragmentQueueBinding, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)

        // --- UI SETUP ---
        binding.queueRecycler.apply {
            adapter = queueAdapter
            touchHelper =
                ItemTouchHelper(QueueDragCallback(queueModel)).also {
                    it.attachToRecyclerView(this)
                }
        }

        // Sometimes the scroll can change without the listener being updated, so we also
        // check for relayout events.
        binding.queueRecycler.apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateDivider() }
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        updateDivider()
                    }
                })
        }

        // --- VIEWMODEL SETUP ----
        collectImmediately(
            queueModel.queue, queueModel.index, playbackModel.isPlaying, ::updateQueue)
    }

    override fun onDestroyBinding(binding: FragmentQueueBinding) {
        super.onDestroyBinding(binding)
        touchHelper = null
        binding.queueRecycler.adapter = null
    }

    override fun onClick(item: Song, viewHolder: RecyclerView.ViewHolder) {
        queueModel.goto(viewHolder.bindingAdapterPosition)
    }

    override fun onPickUp(viewHolder: RecyclerView.ViewHolder) {
        requireNotNull(touchHelper) { "ItemTouchHelper was not available" }.startDrag(viewHolder)
    }

    private fun updateDivider() {
        val binding = requireBinding()
        binding.queueDivider.isInvisible =
            (binding.queueRecycler.layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition() < 1
    }

    private fun updateQueue(queue: List<Song>, index: Int, isPlaying: Boolean) {
        val binding = requireBinding()

        // Replace or diff the queue depending on the type of change it is.
        val instructions = queueModel.queueListInstructions
        queueAdapter.submitList(queue, instructions?.update ?: BasicListInstructions.DIFF)
        // Update position in list (and thus past/future items)
        queueAdapter.setPosition(index, isPlaying)

        // If requested, scroll to a new item (occurs when the index moves)
        val scrollTo = instructions?.scrollTo
        if (scrollTo != null) {
            val lmm = binding.queueRecycler.layoutManager as LinearLayoutManager
            val start = lmm.findFirstCompletelyVisibleItemPosition()
            val end = lmm.findLastCompletelyVisibleItemPosition()
            val notInitialized =
                start == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION
            // When we scroll, we want to scroll to the almost-top so the user can see
            // future songs instead of past songs. The way we have to do this however is
            // dependent on where we have to scroll to get to the currently playing song.
            if (notInitialized || scrollTo < start) {
                // We need to scroll upwards, or initialize the scroll, no need to offset
                binding.queueRecycler.scrollToPosition(scrollTo)
            } else if (scrollTo > end) {
                // We need to scroll downwards, we need to offset by a screen of songs.
                // This does have some error due to how many completely visible items on-screen
                // can vary. This is considered okay.
                binding.queueRecycler.scrollToPosition(
                    min(queue.lastIndex, scrollTo + (end - start)))
            }
        }

        queueModel.finishInstructions()
    }
}
