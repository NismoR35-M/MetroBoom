package com.metroN.boomingC.settings.ui

import android.os.Bundle
import androidx.preference.PreferenceDialogFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.R

/**
 * The companion dialog to [IntListPreference]. Use [from] to create an instance.
 */
class IntListPreferenceDialog : PreferenceDialogFragmentCompat() {
    private val listPreference: IntListPreference
        get() = (preference as IntListPreference)
    private var pendingValueIndex = -1

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        // PreferenceDialogFragmentCompat does not allow us to customize the actual creation
        // of the alert dialog, so we have to override onCreateDialog and create a new dialog
        // ourselves.
        MaterialAlertDialogBuilder(requireContext(), theme)
            .setTitle(listPreference.title)
            .setPositiveButton(null, null)
            .setNegativeButton(R.string.lbl_cancel, null)
            .setSingleChoiceItems(listPreference.entries, listPreference.getValueIndex()) { _, index
                ->
                pendingValueIndex = index
                dismiss()
            }
            .create()

    override fun onDialogClosed(positiveResult: Boolean) {
        if (pendingValueIndex > -1) {
            listPreference.setValueIndex(pendingValueIndex)
        }
    }

    companion object {
        /** The tag to use when instantiating this dialog. */
        const val TAG = BuildConfig.APPLICATION_ID + ".tag.INT_PREF"

        /**
         * Create a new instance.
         * @param preference The [IntListPreference] to display.
         * @return A new instance.
         */
        fun from(preference: IntListPreference) =
            IntListPreferenceDialog().apply {
                // Populate the key field required by PreferenceDialogFragmentCompat.
                arguments = Bundle().apply { putString(ARG_KEY, preference.key) }
            }
    }
}
