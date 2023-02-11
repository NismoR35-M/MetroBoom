package com.metroN.boomingC.settings.categories

import androidx.navigation.fragment.findNavController
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.BasePreferenceFragment
import com.metroN.boomingC.settings.ui.WrappedDialogPreference

/**
 * Personalization settings interface.
 */
class PersonalizePreferenceFragment : BasePreferenceFragment(R.xml.preferences_personalize) {
    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_home_tabs)) {
            findNavController().navigate(PersonalizePreferenceFragmentDirections.goToTabDialog())
        }
    }
}
