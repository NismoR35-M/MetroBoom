package com.metroN.boomingC.image

import com.metroN.boomingC.IntegerTable

/**
 * Represents the options available for album cover loading.
 */
enum class CoverMode {
    /** Do not load album covers ("Off"). */
    OFF,
    /** Load covers from the fast, but lower-quality media store database ("Fast"). */
    MEDIA_STORE,
    /** Load high-quality covers directly from music files ("Quality"). */
    QUALITY;

    /**
     * The integer representation of this instance.
     * @see fromIntCode
     */
    val intCode: Int
        get() =
            when (this) {
                OFF -> IntegerTable.COVER_MODE_OFF
                MEDIA_STORE -> IntegerTable.COVER_MODE_MEDIA_STORE
                QUALITY -> IntegerTable.COVER_MODE_QUALITY
            }

    companion object {
        /**
         * Convert a [CoverMode] integer representation into an instance.
         * @param intCode An integer representation of a [CoverMode]
         * @return The corresponding [CoverMode], or null if the [CoverMode] is invalid.
         * @see CoverMode.intCode
         */
        fun fromIntCode(intCode: Int) =
            when (intCode) {
                IntegerTable.COVER_MODE_OFF -> OFF
                IntegerTable.COVER_MODE_MEDIA_STORE -> MEDIA_STORE
                IntegerTable.COVER_MODE_QUALITY -> QUALITY
                else -> null
            }
    }
}
