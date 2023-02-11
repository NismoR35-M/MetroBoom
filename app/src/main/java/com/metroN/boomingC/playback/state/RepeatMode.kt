package com.metroN.boomingC.playback.state

import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R

/**
 * Represents the current repeat mode of the player.
 */
enum class RepeatMode {
    /**
     * Do not repeat. Songs are played immediately, and playback is paused when the queue repeats.
     */
    NONE,

    /**
     * Repeat the whole queue. Songs are played immediately, and playback continues when the queue
     * repeats.
     */
    ALL,

    /**
     * Repeat the current song. A Song will be continuously played until skipped. If configured,
     * playback may pause when a Song repeats.
     */
    TRACK;

    /**
     * Increment the mode.
     * @return If [NONE], [ALL]. If [ALL], [TRACK]. If [TRACK], [NONE].
     */
    fun increment() =
        when (this) {
            NONE -> ALL
            ALL -> TRACK
            TRACK -> NONE
        }

    /**
     * The integer representation of this instance.
     * @see fromIntCode
     */
    val icon: Int
        get() =
            when (this) {
                NONE -> R.drawable.ic_repeat_off_24
                ALL -> R.drawable.ic_repeat_on_24
                TRACK -> R.drawable.ic_repeat_one_24
            }

    /** The integer code representing this particular mode. */
    val intCode: Int
        get() =
            when (this) {
                NONE -> IntegerTable.REPEAT_MODE_NONE
                ALL -> IntegerTable.REPEAT_MODE_ALL
                TRACK -> IntegerTable.REPEAT_MODE_TRACK
            }

    companion object {
        /**
         * Convert a [RepeatMode] integer representation into an instance.
         * @param intCode An integer representation of a [RepeatMode]
         * @return The corresponding [RepeatMode], or null if the [RepeatMode] is invalid.
         * @see RepeatMode.intCode
         */
        fun fromIntCode(intCode: Int) =
            when (intCode) {
                IntegerTable.REPEAT_MODE_NONE -> NONE
                IntegerTable.REPEAT_MODE_ALL -> ALL
                IntegerTable.REPEAT_MODE_TRACK -> TRACK
                else -> null
            }
    }
}
