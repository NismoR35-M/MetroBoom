package com.metroN.boomingC.playback.persist

import androidx.room.TypeConverter
import com.metroN.boomingC.music.Music

/**
 * Defines conversions used in the persistence table.
 */
object PersistenceConverters {
    /** @see [Music.UID.toString] */
    @TypeConverter fun fromMusicUID(uid: Music.UID?) = uid?.toString()

    /** @see [Music.UID.fromString] */
    @TypeConverter fun toMusicUid(string: String?) = string?.let(Music.UID::fromString)
}
