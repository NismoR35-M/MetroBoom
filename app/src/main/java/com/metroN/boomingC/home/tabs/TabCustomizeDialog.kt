package com.metroN.boomingC.home.tabs

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogTabsBinding
import com.metroN.boomingC.home.HomeSettings
import com.metroN.boomingC.list.EditableListListener
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.logD

/**
 * A [ViewBindingDialogFragment] that allows the user to modify the home [Tab] configuration.
 */
@AndroidEntryPoint
class TabCustomizeDialog :
    ViewBindingDialogFragment<DialogTabsBinding>(), EditableListListener<Tab> {
    private val tabAdapter = TabAdapter(this)
    private var touchHelper: ItemTouchHelper? = null

    override fun onCreateBinding(inflater: LayoutInflater) = DialogTabsBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.set_lib_tabs)
            .setPositiveButton(R.string.lbl_ok) { _, _ ->
                logD("Committing tab changes")
                HomeSettings.from(requireContext()).homeTabs = tabAdapter.tabs
            }
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogTabsBinding, savedInstanceState: Bundle?) {
        var tabs = HomeSettings.from(requireContext()).homeTabs
        // Try to restore a pending tab configuration that was saved prior.
        if (savedInstanceState != null) {
            val savedTabs = Tab.fromIntCode(savedInstanceState.getInt(KEY_TABS))
            if (savedTabs != null) {
                tabs = savedTabs
            }
        }

        // Set up the tab RecyclerView
        tabAdapter.submitTabs(tabs)
        binding.tabRecycler.apply {
            adapter = tabAdapter
            touchHelper =
                ItemTouchHelper(TabDragCallback(tabAdapter)).also { it.attachToRecyclerView(this) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save any pending tab configurations to restore if this dialog is re-created.
        outState.putInt(KEY_TABS, Tab.toIntCode(tabAdapter.tabs))
    }

    override fun onDestroyBinding(binding: DialogTabsBinding) {
        super.onDestroyBinding(binding)
        binding.tabRecycler.adapter = null
    }

    override fun onClick(item: Tab, viewHolder: RecyclerView.ViewHolder) {
        // We will need the exact index of the tab to update on in order to
        // notify the adapter of the change.
        val index = tabAdapter.tabs.indexOfFirst { it.mode == item.mode }
        val tab = tabAdapter.tabs[index]
        tabAdapter.setTab(
            index,
            when (tab) {
                // Invert the visibility of the tab
                is Tab.Visible -> Tab.Invisible(tab.mode)
                is Tab.Invisible -> Tab.Visible(tab.mode)
            })

        // Prevent the user from saving if all the tabs are Invisible, as that's an invalid state.
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            tabAdapter.tabs.filterIsInstance<Tab.Visible>().isNotEmpty()
    }

    override fun onPickUp(viewHolder: RecyclerView.ViewHolder) {
        requireNotNull(touchHelper) { "ItemTouchHelper was not available" }.startDrag(viewHolder)
    }

    private companion object {
        const val KEY_TABS = BuildConfig.APPLICATION_ID + ".key.PENDING_TABS"
    }
}
