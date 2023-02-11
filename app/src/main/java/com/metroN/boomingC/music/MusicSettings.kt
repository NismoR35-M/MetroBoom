
package com.metroN.boomingC.music

import android.content.Context
import android.os.storage.StorageManager
import androidx.core.content.edit
import com.metroN.boomingC.R
import com.metroN.boomingC.list.Sort
import com.metroN.boomingC.music.storage.Directory
import com.metroN.boomingC.music.storage.MusicDirectories
import com.metroN.boomingC.settings.Settings
import com.metroN.boomingC.util.getSystemServiceCompat

/**
 * User configuration specific to music system.
 */
interface MusicSettings : Settings<MusicSettings.Listener> {
    /** The configuration on how to handle particular directories in the music library. */
    var musicDirs: MusicDirectories
    /** Whether to exclude non-music audio files from the music library. */
    val excludeNonMusic: Boolean
    /** Whether to be actively watching for changes in the music library. */
    val shouldBeObserving: Boolean
    /** A [String] of characters representing the desired characters to denote multi-value tags. */
    var multiValueSeparators: String
    /** The [Sort] mode used in [Song] lists. */
    var songSort: Sort
    /** The [Sort] mode used in [Album] lists. */
    var albumSort: Sort
    /** The [Sort] mode used in [Artist] lists. */
    var artistSort: Sort
    /** The [Sort] mode used in [Genre] lists. */
    var genreSort: Sort
    /** The [Sort] mode used in an [Album]'s [Song] list. */
    var albumSongSort: Sort
    /** The [Sort] mode used in an [Artist]'s [Song] list. */
    var artistSongSort: Sort
    /** The [Sort] mode used in an [Genre]'s [Song] list. */
    var genreSongSort: Sort

    interface Listener {
        /** Called when a setting controlling how music is loaded has changed. */
        fun onIndexingSettingChanged() {}
        /** Called when the [shouldBeObserving] configuration has changed. */
        fun onObservingChanged() {}
    }

    companion object {
        /**
         * Get a framework-backed implementation.
         * @param context [Context] required.
         */
        fun from(context: Context): MusicSettings = RealMusicSettings(context)
    }
}

private class RealMusicSettings(context: Context) :
    Settings.Real<MusicSettings.Listener>(context), MusicSettings {
    private val storageManager = context.getSystemServiceCompat(StorageManager::class)

    override var musicDirs: MusicDirectories
        get() {
            val dirs =
                (sharedPreferences.getStringSet(getString(R.string.set_key_music_dirs), null)
                        ?: emptySet())
                    .mapNotNull { Directory.fromDocumentTreeUri(storageManager, it) }
            return MusicDirectories(
                dirs,
                sharedPreferences.getBoolean(getString(R.string.set_key_music_dirs_include), false))
        }
        set(value) {
            sharedPreferences.edit {
                putStringSet(
                    getString(R.string.set_key_music_dirs),
                    value.dirs.map(Directory::toDocumentTreeUri).toSet())
                putBoolean(getString(R.string.set_key_music_dirs_include), value.shouldInclude)
                apply()
            }
        }

    override val excludeNonMusic: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_exclude_non_music), true)

    override val shouldBeObserving: Boolean
        get() = sharedPreferences.getBoolean(getString(R.string.set_key_observing), false)

    override var multiValueSeparators: String
        // Differ from convention and store a string of separator characters instead of an int
        // code. This makes it easier to use and more extendable.
        get() = sharedPreferences.getString(getString(R.string.set_key_separators), "") ?: ""
        set(value) {
            sharedPreferences.edit {
                putString(getString(R.string.set_key_separators), value)
                apply()
            }
        }

    override var songSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_songs_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_songs_sort), value.intCode)
                apply()
            }
        }

    override var albumSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_albums_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_albums_sort), value.intCode)
                apply()
            }
        }

    override var artistSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_artists_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_artists_sort), value.intCode)
                apply()
            }
        }

    override var genreSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(getString(R.string.set_key_genres_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_genres_sort), value.intCode)
                apply()
            }
        }

    override var albumSongSort: Sort
        get() {
            var sort =
                Sort.fromIntCode(
                    sharedPreferences.getInt(
                        getString(R.string.set_key_album_songs_sort), Int.MIN_VALUE))
                    ?: Sort(Sort.Mode.ByDisc, Sort.Direction.ASCENDING)

            // Correct legacy album sort modes to Disc
            if (sort.mode is Sort.Mode.ByName) {
                sort = sort.withMode(Sort.Mode.ByDisc)
            }

            return sort
        }
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_album_songs_sort), value.intCode)
                apply()
            }
        }

    override var artistSongSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(
                    getString(R.string.set_key_artist_songs_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByDate, Sort.Direction.DESCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_artist_songs_sort), value.intCode)
                apply()
            }
        }

    override var genreSongSort: Sort
        get() =
            Sort.fromIntCode(
                sharedPreferences.getInt(
                    getString(R.string.set_key_genre_songs_sort), Int.MIN_VALUE))
                ?: Sort(Sort.Mode.ByName, Sort.Direction.ASCENDING)
        set(value) {
            sharedPreferences.edit {
                putInt(getString(R.string.set_key_genre_songs_sort), value.intCode)
                apply()
            }
        }

    override fun onSettingChanged(key: String, listener: MusicSettings.Listener) {
        when (key) {
            getString(R.string.set_key_exclude_non_music),
            getString(R.string.set_key_music_dirs),
            getString(R.string.set_key_music_dirs_include),
            getString(R.string.set_key_separators) -> listener.onIndexingSettingChanged()
            getString(R.string.set_key_observing) -> listener.onObservingChanged()
        }
    }
}
