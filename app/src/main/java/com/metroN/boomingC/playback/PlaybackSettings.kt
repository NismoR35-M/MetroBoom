package com.metroN.boomingC.playback

import android.content.Context
import androidx.core.content.edit
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R
import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.playback.replaygain.ReplayGainMode
import com.metroN.boomingC.playback.replaygain.ReplayGainPreAmp
import com.metroN.boomingC.settings.Settings
import com.metroN.boomingC.util.logD

/**
 * User configuration specific to the playback system.
 */
interface PlaybackSettings : Settings<PlaybackSettings.Listener> {
    /** The action to display on the playback bar. */
    val barAction: ActionMode
    /** The action to display in the playback notification. */
    val notificationAction: ActionMode
    /** Whether to start playback when a headset is plugged in. */
    val headsetAutoplay: Boolean
    /** The current ReplayGain configuration. */
    val replayGainMode: ReplayGainMode
    /** The current ReplayGain pre-amp configuration. */
    var replayGainPreAmp: ReplayGainPreAmp
    /**
     * What type of MusicParent to play from when a Song is played from a list of other items. Null
     * if to play from all Songs.
     */
    val inListPlaybackMode: MusicMode
    /**
     * What type of MusicParent to play from when a Song is played from within an item (ex. like in
     * the detail view). Null if to play from the item it was played in.
     */
    val inParentPlaybackMode: MusicMode?
    /** Whether to keep shuffle on when playing a new Song. */
    val keepShuffle: Boolean
    /** Whether to rewind when the skip previous button is pressed before skipping back. */
    val rewindWithPrev: Boolean
    /** Whether a song should pause after every repeat. */
    val pauseOnRepeat: Boolean

    interface Listener {
        /** Called when one of the ReplayGain configurations have changed. */
        fun onReplayGainSettingsChanged() {}
        /** Called when [notificationAction] has changed. */
        fun onNotificationActionChanged() {}
    }

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): PlaybackSettings = RealPlaybackSettings(context)
    }
}

private class RealPlaybackSettings(context: Context) :
    Settings.Real<PlaybackSettings.Listener>(context), PlaybackSettings {
    override val inListPlaybackMode: MusicMode
        get() =
            MusicMode.fromIntCode(
                sharedPreferences.getInt(
                    getString(R.string.set_key_in_list_playback_mode), Int.MIN_VALUE))
                ?: MusicMode.SONGS

    override val inParentPlaybackMode: MusicMode?
        get() =
            MusicMode.fromIntCode(
                sharedPreferences.getInt(
                    getString(R.string.set_key_in_parent_playback_mode), Int.MIN_VALUE))

    override val barAction: ActionMode
        get() =
            ActionMode.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_bar_action), Int.MIN_VALUE))
                ?: ActionMode.NEXT

    override val notificationAction: ActionMode
        get() =
            ActionMode.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_notif_action), Int.MIN_VALUE))
                ?: ActionMode.REPEAT

    override val headsetAutoplay: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_headset_autoplay), false)

    override val replayGainMode: ReplayGainMode
        get() =
            ReplayGainMode.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_replay_gain), Int.MIN_VALUE))
                ?: ReplayGainMode.DYNAMIC

    override var replayGainPreAmp: ReplayGainPreAmp
        get() =
            ReplayGainPreAmp(
                sharedPreferences.getFloat(getString(R.string.set_key_pre_amp_with), 0f),
                sharedPreferences.getFloat(getString(R.string.set_key_pre_amp_without), 0f))
        set(value) {
            sharedPreferences.edit {
                putFloat(getString(R.string.set_key_pre_amp_with), value.with)
                putFloat(getString(R.string.set_key_pre_amp_without), value.without)
                apply()
            }
        }

    override val keepShuffle: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_keep_shuffle), true)

    override val rewindWithPrev: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_rewind_prev), true)

    override val pauseOnRepeat: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_repeat_pause), false)

    override fun migrate() {
        // "Use alternate notification action" was converted to an ActionMode setting in 3.0.0.
        if (sharedPreferences.contains(OLD_KEY_ALT_NOTIF_ACTION)) {
            logD("Migrating $OLD_KEY_ALT_NOTIF_ACTION")

            val mode =
                if (sharedPreferences.getBoolean(OLD_KEY_ALT_NOTIF_ACTION, false)) {
                    ActionMode.SHUFFLE
                } else {
                    ActionMode.REPEAT
                }

            sharedPreferences.edit {
                putInt(getString(R.string.set_key_notif_action), mode.intCode)
                remove(OLD_KEY_ALT_NOTIF_ACTION)
                apply()
            }
        }

        // PlaybackMode was converted to MusicMode in 3.0.0

        fun Int.migratePlaybackMode() =
            when (this) {
                // Convert PlaybackMode into MusicMode
                IntegerTable.PLAYBACK_MODE_ALL_SONGS -> MusicMode.SONGS
                IntegerTable.PLAYBACK_MODE_IN_ARTIST -> MusicMode.ARTISTS
                IntegerTable.PLAYBACK_MODE_IN_ALBUM -> MusicMode.ALBUMS
                IntegerTable.PLAYBACK_MODE_IN_GENRE -> MusicMode.GENRES
                else -> null
            }

        if (sharedPreferences.contains(OLD_KEY_LIB_PLAYBACK_MODE)) {
            logD("Migrating $OLD_KEY_LIB_PLAYBACK_MODE")

            val mode =
                sharedPreferences
                    .getInt(OLD_KEY_LIB_PLAYBACK_MODE, IntegerTable.PLAYBACK_MODE_ALL_SONGS)
                    .migratePlaybackMode()
                    ?: MusicMode.SONGS

            sharedPreferences.edit {
                putInt(getString(R.string.set_key_in_list_playback_mode), mode.intCode)
                remove(OLD_KEY_LIB_PLAYBACK_MODE)
                apply()
            }
        }

        if (sharedPreferences.contains(OLD_KEY_DETAIL_PLAYBACK_MODE)) {
            logD("Migrating $OLD_KEY_DETAIL_PLAYBACK_MODE")

            val mode =
                sharedPreferences
                    .getInt(OLD_KEY_DETAIL_PLAYBACK_MODE, Int.MIN_VALUE)
                    .migratePlaybackMode()

            sharedPreferences.edit {
                putInt(
                    getString(R.string.set_key_in_parent_playback_mode),
                    mode?.intCode ?: Int.MIN_VALUE)
                remove(OLD_KEY_DETAIL_PLAYBACK_MODE)
                apply()
            }
        }
    }

    override fun onSettingChanged(key: String, listener: PlaybackSettings.Listener) {
        when (key) {
            getString(R.string.set_key_replay_gain),
            getString(R.string.set_key_pre_amp_with),
            getString(R.string.set_key_pre_amp_without) -> listener.onReplayGainSettingsChanged()
            getString(R.string.set_key_notif_action) -> listener.onNotificationActionChanged()
        }
    }

    private companion object {
        const val OLD_KEY_ALT_NOTIF_ACTION = "KEY_ALT_NOTIF_ACTION"
        const val OLD_KEY_LIB_PLAYBACK_MODE = "KEY_SONG_PLAY_MODE2"
        const val OLD_KEY_DETAIL_PLAYBACK_MODE = "auxio_detail_song_play_mode"
    }
}
