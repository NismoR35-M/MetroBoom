package com.metroN.boomingC.list

import androidx.annotation.StringRes

/** A marker for something that is a RecyclerView item. Has no functionality on it's own. */
interface Item

/**
 * A "header" used for delimiting groups of data.
 */
interface Header : Item {
    /** The string resource used for the header's title. */
    val titleRes: Int
}

/**
 * A basic header with no additional actions.
 * @param titleRes The string resource used for the header's title.
 * @author Alexander Capehart (OxygenCobalt)
 */
data class BasicHeader(@StringRes override val titleRes: Int) : Header
