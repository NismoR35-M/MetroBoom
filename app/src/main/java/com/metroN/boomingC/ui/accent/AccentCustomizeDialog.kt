package com.metroN.boomingC.ui.accent

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogAccentBinding
import com.metroN.boomingC.list.ClickableListListener
import com.metroN.boomingC.ui.UISettings
import com.metroN.boomingC.ui.ViewBindingDialogFragment
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.unlikelyToBeNull
import dagger.hilt.android.AndroidEntryPoint

/** A [ViewBindingDialogFragment] that allows the user to configure the current [Accent]. */
@AndroidEntryPoint
class AccentCustomizeDialog :
    ViewBindingDialogFragment<DialogAccentBinding>(), ClickableListListener<Accent> {
    private var accentAdapter = AccentAdapter(this)

    override fun onCreateBinding(inflater: LayoutInflater) = DialogAccentBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.set_accent)
            .setPositiveButton(R.string.lbl_ok) { _, _ ->
                val settings = UISettings.from(requireContext())
                if (accentAdapter.selectedAccent == settings.accent) {
                    // Nothing to do.
                    return@setPositiveButton
                }

                logD("Applying new accent")
                settings.accent = unlikelyToBeNull(accentAdapter.selectedAccent)
                requireActivity().recreate()
                dismiss()
            }
            .setNegativeButton(R.string.lbl_cancel, null)
    }

    override fun onBindingCreated(binding: DialogAccentBinding, savedInstanceState: Bundle?) {
        binding.accentRecycler.adapter = accentAdapter
        // Restore a previous pending accent if possible, otherwise select the current setting.
        accentAdapter.setSelectedAccent(
            if (savedInstanceState != null) {
                Accent.from(savedInstanceState.getInt(KEY_PENDING_ACCENT))
            } else {
                UISettings.from(requireContext()).accent
            })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save any pending accent configuration to restore if this dialog is re-created.
        outState.putInt(KEY_PENDING_ACCENT, unlikelyToBeNull(accentAdapter.selectedAccent).index)
    }

    override fun onDestroyBinding(binding: DialogAccentBinding) {
        binding.accentRecycler.adapter = null
    }

    override fun onClick(item: Accent, viewHolder: RecyclerView.ViewHolder) {
        accentAdapter.setSelectedAccent(item)
    }

    private companion object {
        const val KEY_PENDING_ACCENT = BuildConfig.APPLICATION_ID + ".key.PENDING_ACCENT"
    }
}
