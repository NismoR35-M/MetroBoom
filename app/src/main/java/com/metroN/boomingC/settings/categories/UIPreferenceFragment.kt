package com.metroN.boomingC.settings.categories

import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.BasePreferenceFragment
import com.metroN.boomingC.settings.ui.WrappedDialogPreference
import com.metroN.boomingC.ui.UISettings
import com.metroN.boomingC.util.isNight

/**
 * Display preferences.
 */
class UIPreferenceFragment : BasePreferenceFragment(R.xml.preferences_ui) {
    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_accent)) {
            findNavController().navigate(UIPreferenceFragmentDirections.goToAccentDialog())
        }
    }

    override fun onSetupPreference(preference: Preference) {
        when (preference.key) {
            getString(R.string.set_key_theme) -> {
                preference.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, value ->
                        AppCompatDelegate.setDefaultNightMode(value as Int)
                        true
                    }
            }
            getString(R.string.set_key_accent) -> {
                preference.summary = getString(UISettings.from(requireContext()).accent.name)
            }
            getString(R.string.set_key_black_theme) -> {
                preference.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, _ ->
                        val activity = requireActivity()
                        if (activity.isNight) {
                            activity.recreate()
                        }

                        true
                    }
            }
        }
    }
}
