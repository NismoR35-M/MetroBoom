package com.metroN.boomingC.list.selection

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import com.metroN.boomingC.R
import com.metroN.boomingC.playback.PlaybackViewModel
import com.metroN.boomingC.ui.ViewBindingFragment
import com.metroN.boomingC.util.showToast

/**
 * A subset of ListFragment that implements aspects of the selection UI.
 */
abstract class SelectionFragment<VB : ViewBinding> :
    ViewBindingFragment<VB>(), Toolbar.OnMenuItemClickListener {
    protected abstract val selectionModel: SelectionViewModel
    protected abstract val playbackModel: PlaybackViewModel

    /**
     * Get the [SelectionToolbarOverlay] of the concrete Fragment to be automatically managed by
     * [SelectionFragment].
     * @return The [SelectionToolbarOverlay] of the concrete [SelectionFragment]'s [VB], or null if
     * there is not one.
     */
    open fun getSelectionToolbar(binding: VB): SelectionToolbarOverlay? = null

    override fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) {
        super.onBindingCreated(binding, savedInstanceState)
        getSelectionToolbar(binding)?.apply {
            // Add cancel and menu item listeners to manage what occurs with the selection.
            setOnSelectionCancelListener { selectionModel.consume() }
            setOnMenuItemClickListener(this@SelectionFragment)
        }
    }

    override fun onDestroyBinding(binding: VB) {
        super.onDestroyBinding(binding)
        getSelectionToolbar(binding)?.setOnMenuItemClickListener(null)
    }

    override fun onMenuItemClick(item: MenuItem) =
        when (item.itemId) {
            R.id.action_selection_play_next -> {
                playbackModel.playNext(selectionModel.consume())
                requireContext().showToast(R.string.lng_queue_added)
                true
            }
            R.id.action_selection_queue_add -> {
                playbackModel.addToQueue(selectionModel.consume())
                requireContext().showToast(R.string.lng_queue_added)
                true
            }
            R.id.action_selection_play -> {
                playbackModel.play(selectionModel.consume())
                true
            }
            R.id.action_selection_shuffle -> {
                playbackModel.shuffle(selectionModel.consume())
                true
            }
            else -> false
        }
}
