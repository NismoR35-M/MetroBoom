package com.metroN.boomingC.music

import com.metroN.boomingC.IntegerTable

/**
 * Represents a data configuration corresponding to a specific type of [Music],
 */
enum class MusicMode {
    /** Configure with respect to [Song] instances. */
    SONGS,
    /** Configure with respect to [Album] instances. */
    ALBUMS,
    /** Configure with respect to [Artist] instances. */
    ARTISTS,
    /** Configure with respect to [Genre] instances. */
    GENRES;

    /**
     * The integer representation of this instance.
     * @see fromIntCode
     */
    val intCode: Int
        get() =
            when (this) {
                SONGS -> IntegerTable.MUSIC_MODE_SONGS
                ALBUMS -> IntegerTable.MUSIC_MODE_ALBUMS
                ARTISTS -> IntegerTable.MUSIC_MODE_ARTISTS
                GENRES -> IntegerTable.MUSIC_MODE_GENRES
            }

    companion object {
        /**
         * Convert a [MusicMode] integer representation into an instance.
         * @param intCode An integer representation of a [MusicMode]
         * @return The corresponding [MusicMode], or null if the [MusicMode] is invalid.
         * @see MusicMode.intCode
         */
        fun fromIntCode(intCode: Int) =
            when (intCode) {
                IntegerTable.MUSIC_MODE_SONGS -> SONGS
                IntegerTable.MUSIC_MODE_ALBUMS -> ALBUMS
                IntegerTable.MUSIC_MODE_ARTISTS -> ARTISTS
                IntegerTable.MUSIC_MODE_GENRES -> GENRES
                else -> null
            }
    }
}
