package com.metroN.boomingC.music.metadata

import com.metroN.boomingC.list.Item

/**
 * A disc identifier for a song.
 * @param number The disc number.
 * @param name The name of the disc group, if any. Null if not present.
 */
class Disc(val number: Int, val name: String?) : Item, Comparable<Disc> {
    override fun hashCode() = number.hashCode()
    override fun equals(other: Any?) = other is Disc && number == other.number
    override fun compareTo(other: Disc) = number.compareTo(other.number)
}
