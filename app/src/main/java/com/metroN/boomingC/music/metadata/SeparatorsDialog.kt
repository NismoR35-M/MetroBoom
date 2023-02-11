package com.metroN.boomingC.music.metadata

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.material.checkbox.MaterialCheckBox
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R
import com.metroN.boomingC.databinding.DialogSeparatorsBinding
import com.metroN.boomingC.music.MusicSettings
import com.metroN.boomingC.ui.ViewBindingDialogFragment

/**
 * A [ViewBindingDialogFragment] that allows the user to configure the separator characters used to
 * split tags with multiple values.
 */
class SeparatorsDialog : ViewBindingDialogFragment<DialogSeparatorsBinding>() {
    override fun onCreateBinding(inflater: LayoutInflater) =
        DialogSeparatorsBinding.inflate(inflater)

    override fun onConfigDialog(builder: AlertDialog.Builder) {
        builder
            .setTitle(R.string.set_separators)
            .setNegativeButton(R.string.lbl_cancel, null)
            .setPositiveButton(R.string.lbl_save) { _, _ ->
                MusicSettings.from(requireContext()).multiValueSeparators = getCurrentSeparators()
            }
    }

    override fun onBindingCreated(binding: DialogSeparatorsBinding, savedInstanceState: Bundle?) {
        for (child in binding.separatcomroup.children) {
            if (child is MaterialCheckBox) {
                // Reset the CheckBox state so that we can ensure that state we load in
                // from settings is not contaminated from the built-in CheckBox saved state.
                child.isChecked = false
            }
        }

        // More efficient to do one iteration through the separator list and initialize
        // the corresponding CheckBox for each character instead of doing an iteration
        // through the separator list for each CheckBox.
        (savedInstanceState?.getString(KEY_PENDING_SEPARATORS)
                ?: MusicSettings.from(requireContext()).multiValueSeparators)
            .forEach {
                when (it) {
                    Separators.COMMA -> binding.separatorComma.isChecked = true
                    Separators.SEMICOLON -> binding.separatorSemicolon.isChecked = true
                    Separators.SLASH -> binding.separatorSlash.isChecked = true
                    Separators.PLUS -> binding.separatorPlus.isChecked = true
                    Separators.AND -> binding.separatorAnd.isChecked = true
                    else -> error("Unexpected separator in settings data")
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PENDING_SEPARATORS, getCurrentSeparators())
    }

    /** Get the current separator string configuration from the UI. */
    private fun getCurrentSeparators(): String {
        // Create the separator list based on the checked configuration of each
        // view element. It's generally more stable to duplicate this code instead
        // of use a mapping that could feasibly drift from the actual layout.
        var separators = ""
        val binding = requireBinding()
        if (binding.separatorComma.isChecked) separators += Separators.COMMA
        if (binding.separatorSemicolon.isChecked) separators += Separators.SEMICOLON
        if (binding.separatorSlash.isChecked) separators += Separators.SLASH
        if (binding.separatorPlus.isChecked) separators += Separators.PLUS
        if (binding.separatorAnd.isChecked) separators += Separators.AND
        return separators
    }

    private companion object {
        const val KEY_PENDING_SEPARATORS = BuildConfig.APPLICATION_ID + ".key.PENDING_SEPARATORS"
    }
}
