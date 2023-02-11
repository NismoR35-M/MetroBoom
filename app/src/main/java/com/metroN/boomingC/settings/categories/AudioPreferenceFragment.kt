package com.metroN.boomingC.settings.categories

import androidx.navigation.fragment.findNavController
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.BasePreferenceFragment
import com.metroN.boomingC.settings.ui.WrappedDialogPreference

/**
 * Audio settings interface.
 */
class AudioPreferenceFragment : BasePreferenceFragment(R.xml.preferences_audio) {

    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_pre_amp)) {
            findNavController().navigate(AudioPreferenceFragmentDirections.goToPreAmpDialog())
        }
    }
}
