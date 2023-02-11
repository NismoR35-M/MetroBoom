package com.metroN.boomingC.image

import android.content.Context
import androidx.core.content.edit
import com.metroN.boomingC.R
import com.metroN.boomingC.settings.Settings
import com.metroN.boomingC.util.logD

/**
 * User configuration specific to image loading.
 */
interface ImageSettings : Settings<ImageSettings.Listener> {
    /** The strategy to use when loading album covers. */
    val coverMode: CoverMode

    interface Listener {
        /** Called when [coverMode] changes. */
        fun onCoverModeChanged() {}
    }

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): ImageSettings = RealImageSettings(context)
    }
}

private class RealImageSettings(context: Context) :
    Settings.Real<ImageSettings.Listener>(context), ImageSettings {
    override val coverMode: CoverMode
        get() =
            CoverMode.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_cover_mode), Int.MIN_VALUE))
                ?: CoverMode.MEDIA_STORE

    override fun migrate() {
        // Show album covers and Ignore MediaStore covers were unified in 3.0.0
        if (sharedPreferences.contains(OLD_KEY_SHOW_COVERS) ||
            sharedPreferences.contains(OLD_KEY_QUALITY_COVERS)) {
            logD("Migrating cover settings")

            val mode =
                when {
                    !sharedPreferences.getBoolean(OLD_KEY_SHOW_COVERS, true) -> CoverMode.OFF
                    !sharedPreferences.getBoolean(OLD_KEY_QUALITY_COVERS, true) ->
                        CoverMode.MEDIA_STORE
                    else -> CoverMode.QUALITY
                }

            sharedPreferences.edit {
                putInt(getString(R.string.set_key_cover_mode), mode.intCode)
                remove(OLD_KEY_SHOW_COVERS)
                remove(OLD_KEY_QUALITY_COVERS)
            }
        }
    }

    override fun onSettingChanged(key: String, listener: ImageSettings.Listener) {
        if (key == getString(R.string.set_key_cover_mode)) {
            listener.onCoverModeChanged()
        }
    }

    private companion object {
        const val OLD_KEY_SHOW_COVERS = "KEY_SHOW_COVERS"
        const val OLD_KEY_QUALITY_COVERS = "KEY_QUALITY_COVERS"
    }
}
