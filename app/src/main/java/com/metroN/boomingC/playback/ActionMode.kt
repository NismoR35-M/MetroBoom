package com.metroN.boomingC.playback

import com.metroN.boomingC.IntegerTable

/**
 * Represents a configuration option for what kind of "secondary" action to show in a particular UI
 * context.
 */
enum class ActionMode {
    /** Use a "Skip next" button for the secondary action. */
    NEXT,
    /** Use a repeat mode button for the secondary action. */
    REPEAT,
    /** Use a shuffle mode button for the secondary action. */
    SHUFFLE;

    /**
     * The integer representation of this instance.
     * @see fromIntCode
     */
    val intCode: Int
        get() =
            when (this) {
                NEXT -> IntegerTable.ACTION_MODE_NEXT
                REPEAT -> IntegerTable.ACTION_MODE_REPEAT
                SHUFFLE -> IntegerTable.ACTION_MODE_SHUFFLE
            }

    companion object {
        /**
         * Convert a [ActionMode] integer representation into an instance.
         * @param intCode An integer representation of a [ActionMode]
         * @return The corresponding [ActionMode], or null if the [ActionMode] is invalid.
         * @see ActionMode.intCode
         */
        fun fromIntCode(intCode: Int) =
            when (intCode) {
                IntegerTable.ACTION_MODE_NEXT -> NEXT
                IntegerTable.ACTION_MODE_REPEAT -> REPEAT
                IntegerTable.ACTION_MODE_SHUFFLE -> SHUFFLE
                else -> null
            }
    }
}
