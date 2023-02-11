package com.metroN.boomingC.home

import android.content.Context
import androidx.core.content.edit
import com.metroN.boomingC.R
import com.metroN.boomingC.home.tabs.Tab
import com.metroN.boomingC.settings.Settings
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * User configuration specific to the home UI.
 */
interface HomeSettings : Settings<HomeSettings.Listener> {
    /** The tabs to show in the home UI. */
    var homeTabs: Array<Tab>
    /** Whether to hide artists considered "collaborators" from the home UI. */
    val shouldHideCollaborators: Boolean

    interface Listener {
        /** Called when the [homeTabs] configuration changes. */
        fun onTabsChanged()
        /** Called when the [shouldHideCollaborators] configuration changes. */
        fun onHideCollaboratorsChanged()
    }

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): HomeSettings = RealHomeSettings(context)
    }
}

private class RealHomeSettings(context: Context) :
    Settings.Real<HomeSettings.Listener>(context), HomeSettings {
    override var homeTabs: Array<Tab>
        get() =
            Tab.fromIntCode(
                sharedPreferences.getInt(
                    getString(R.string.set_key_home_tabs), Tab.SEQUENCE_DEFAULT))
                ?: unlikelyToBeNull(Tab.fromIntCode(Tab.SEQUENCE_DEFAULT))
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_home_tabs), Tab.toIntCode(value))
                apply()
            }
        }

    override val shouldHideCollaborators: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_hide_collaborators), false)

    override fun onSettingChanged(key: String, listener: HomeSettings.Listener) {
        when (key) {
            getString(R.string.set_key_home_tabs) -> listener.onTabsChanged()
            getString(R.string.set_key_hide_collaborators) -> listener.onHideCollaboratorsChanged()
        }
    }
}
