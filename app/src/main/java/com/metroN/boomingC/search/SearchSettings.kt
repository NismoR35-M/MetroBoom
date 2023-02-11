package com.metroN.boomingC.search

import android.content.Context
import androidx.core.content.edit
import com.metroN.boomingC.R
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.settings.Settings

/**
 * User configuration specific to the search UI.
 */
interface SearchSettings : Settings<Nothing> {
    /** The type of Music the search view is currently filtering to. */
    var searchFilterMode: MusicMode?

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): SearchSettings = RealSearchSettings(context)
    }
}

private class RealSearchSettings(context: Context) :
    Settings.Real<Nothing>(context), SearchSettings {
    override var searchFilterMode: MusicMode?
        get() =
            MusicMode.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_search_filter), Int.MIN_VALUE))
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_search_filter), value?.intCode ?: Int.MIN_VALUE)
                apply()
            }
        }
}
