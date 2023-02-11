/*
 * Copyright (c) 2023 Auxio Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.metroN.boomingC.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.metroN.boomingC.util.logW
import com.metroN.boomingC.util.unlikelyToBeNull

/**
 * Abstract user configuration information. This interface has no functionality whatsoever. Concrete
 * implementations should be preferred instead.
 */
interface Settings<L> {
    /**
     * Migrate any settings fields from older versions into their new counterparts.
     * @throws NotImplementedError If there is nothing to migrate.
     */
    fun migrate() {
        throw NotImplementedError()
    }

    /**
     * Add a listener to monitor for settings updates. Will do nothing if
     * @param listener The listener to add.
     */
    fun registerListener(listener: L)

    /**
     * Unregister a listener, preventing any further settings updates from being sent to it.
     * @param listener The listener to unregister, must be the same as the current listener.
     */
    fun unregisterListener(listener: L)

    /**
     * A framework-backed [Settings] implementation.
     * @param context [Context] required.
     */
    abstract class Real<L>(private val context: Context) :
        Settings<L>, SharedPreferences.OnSharedPreferenceChangeListener {
        protected val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

        /** @see [Context.getString] */
        protected fun getString(@StringRes stringRes: Int) = context.getString(stringRes)

        private var listener: L? = null

        override fun registerListener(listener: L) {
            if (this.listener == null) {
                // Registering a listener when it was null prior, attach the callback.
                sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            }
            this.listener = listener
        }

        override fun unregisterListener(listener: L) {
            if (this.listener !== listener) {
                logW("Given listener was not the current listener.")
            }
            this.listener = null
            // No longer have a listener, detach from the preferences instance.
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        final override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences,
            key: String
        ) {
            onSettingChanged(key, unlikelyToBeNull(listener))
        }

        /**
         * Called when a setting entry with the given [key] has changed.
         * @param key The key of the changed setting.
         * @param listener The implementation's listener that updates should be applied to.
         */
        protected open fun onSettingChanged(key: String, listener: L) {}
    }
}
