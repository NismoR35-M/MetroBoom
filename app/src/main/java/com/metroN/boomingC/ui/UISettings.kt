package com.metroN.boomingC.ui

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.Settings
import com.metroN.boomingC.ui.accent.Accent
import com.metroN.boomingC.util.logD

interface UISettings : Settings<UISettings.Listener> {
    /** The current theme. Represented by the AppCompatDelegate constants. */
    val theme: Int
    /** Whether to use a black background when a dark theme is currently used. */
    val useBlackTheme: Boolean
    /** The current [Accent] (Color Scheme). */
    var accent: Accent
    /** Whether to round additional UI elements that require album covers to be rounded. */
    val roundMode: Boolean

    interface Listener {
        /** Called when [roundMode] changes. */
        fun onRoundModeChanged()
    }

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): UISettings = RealUISettings(context)
    }
}

private class RealUISettings(context: Context) :
    Settings.Real<UISettings.Listener>(context), UISettings {
    override val theme: Int
        get() =
            sharedPreferences.getInt(
                getString(R.string.set_key_theme), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    override val useBlackTheme: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_black_theme), false)

    override var accent: Accent
        get() =
            Accent.from(
                sharedPreferences.getInt(getString(R.string.set_key_accent), Accent.DEFAULT))
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_accent), value.index)
                apply()
            }
        }

    override val roundMode: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_round_mode), false)

    override fun migrate() {
        if (sharedPreferences.contains(OLD_KEY_ACCENT3)) {
            logD("Migrating $OLD_KEY_ACCENT3")

            var accent = sharedPreferences.getInt(OLD_KEY_ACCENT3, 5)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Accents were previously frozen as soon as the OS was updated to android
                // twelve, as dynamic colors were enabled by default. This is no longer the
                // case, so we need to re-update the setting to dynamic colors here.
                accent = 16
            }

            sharedPreferences.edit {
                putInt(getString(R.string.set_key_accent), accent)
                remove(OLD_KEY_ACCENT3)
                apply()
            }
        }
    }

    override fun onSettingChanged(key: String, listener: UISettings.Listener) {
        if (key == getString(R.string.set_key_round_mode)) {
            listener.onRoundModeChanged()
        }
    }

    private companion object {
        const val OLD_KEY_ACCENT3 = "auxio_accent"
    }
}
