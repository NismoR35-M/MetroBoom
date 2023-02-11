
package com.metroN.boomingC.settings.categories

import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import coil.Coil
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.BasePreferenceFragment
import com.metroN.boomingC.settings.ui.WrappedDialogPreference

/**
 * "Content" settings.
 */
class MusicPreferenceFragment : BasePreferenceFragment(R.xml.preferences_music) {
    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_separators)) {
            findNavController().navigate(MusicPreferenceFragmentDirections.goToSeparatorsDialog())
        }
    }

    override fun onSetupPreference(preference: Preference) {
        if (preference.key == getString(R.string.set_key_cover_mode)) {
            preference.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, _ ->
                    Coil.imageLoader(requireContext()).memoryCache?.clear()
                    true
                }
        }
    }
}
