package com.metroN.boomingC.home.tabs

import com.metroN.boomingC.music.MusicMode
import com.metroN.boomingC.util.logE

/**
 * A representation of a library tab suitable for configuration.
 * @param mode The type of list in the home view this instance corresponds to.
 */
sealed class Tab(open val mode: MusicMode) {
    /**
     * A visible tab. This will be visible in the home and tab configuration views.
     * @param mode The type of list in the home view this instance corresponds to.
     */
    data class Visible(override val mode: MusicMode) : Tab(mode)

    /**
     * A visible tab. This will be visible in the tab configuration view, but not in the home view.
     * @param mode The type of list in the home view this instance corresponds to.
     */
    data class Invisible(override val mode: MusicMode) : Tab(mode)

    companion object {
        // Like other IO-bound datatypes in Auxio, tabs are stored in a binary format. However, tabs
        // cannot be serialized on their own. Instead, they are saved as a sequence of tabs as shown
        // below:
        //
        // 0bTAB1_TAB2_TAB3_TAB4_TAB5
        //
        // Where TABN is a chunk representing a tab at position N. TAB5 is reserved for playlists.
        // Each chunk in a sequence is represented as:
        //
        // VTTT
        //
        // Where V is a bit representing the visibility and T is a 3-bit integer representing the
        // MusicMode for this tab.

        /** The length a well-formed tab sequence should be. */
        private const val SEQUENCE_LEN = 4

        /**
         * The default tab sequence, in integer form. This represents a set of four visible tabs
         * ordered as "Song", "Album", "Artist", and "Genre".
         */
        const val SEQUENCE_DEFAULT = 0b1000_1001_1010_1011_0100

        /** Maps between the integer code in the tab sequence and it's [MusicMode]. */
        private val MODE_TABLE =
            arrayOf(MusicMode.SONGS, MusicMode.ALBUMS, MusicMode.ARTISTS, MusicMode.GENRES)

        /**
         * Convert an array of [Tab]s into it's integer representation.
         * @param tabs The array of [Tab]s to convert
         * @return An integer representation of the [Tab] array
         */
        fun toIntCode(tabs: Array<Tab>): Int {
            // Like when deserializing, make sure there are no duplicate tabs for whatever reason.
            val distinct = tabs.distinctBy { it.mode }

            var sequence = 0b0100
            var shift = SEQUENCE_LEN * 4
            for (tab in distinct) {
                val bin =
                    when (tab) {
                        is Visible -> 1.shl(3) or MODE_TABLE.indexOf(tab.mode)
                        is Invisible -> MODE_TABLE.indexOf(tab.mode)
                    }

                sequence = sequence or bin.shl(shift)
                shift -= 4
            }

            return sequence
        }

        /**
         * Convert a [Tab] integer representation into it's corresponding array of [Tab]s.
         * @param intCode The integer representation of the [Tab]s.
         * @return An array of [Tab]s corresponding to the sequence.
         */
        fun fromIntCode(intCode: Int): Array<Tab>? {
            val tabs = mutableListOf<Tab>()

            // Try to parse a mode for each chunk in the sequence.
            // If we can't parse one, just skip it.
            for (shift in (0..4 * SEQUENCE_LEN).reversed() step 4) {
                val chunk = intCode.shr(shift) and 0b1111

                val mode = MODE_TABLE.getOrNull(chunk and 7) ?: continue

                // Figure out the visibility
                tabs +=
                    if (chunk and 1.shl(3) != 0) {
                        Visible(mode)
                    } else {
                        Invisible(mode)
                    }
            }

            // Make sure there are no duplicate tabs
            val distinct = tabs.distinctBy { it.mode }

            // For safety, return null if we have an empty or larger-than-expected tab array.
            if (distinct.isEmpty() || distinct.size < SEQUENCE_LEN) {
                logE("Sequence size was ${distinct.size}, which is invalid")
                return null
            }

            return distinct.toTypedArray()
        }
    }
}
