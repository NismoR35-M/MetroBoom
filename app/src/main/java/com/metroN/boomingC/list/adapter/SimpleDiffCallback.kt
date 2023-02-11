package com.metroN.boomingC.list.adapter

import androidx.recyclerview.widget.DiffUtil
import com.metroN.boomingC.list.Item

/**
 * A [DiffUtil.ItemCallback] that automatically implements the [areItemsTheSame] method. Use this
 * whenever creating [DiffUtil.ItemCallback] implementations with an [Item] subclass.
 */
abstract class SimpleDiffCallback<T : Item> : DiffUtil.ItemCallback<T>() {
    final override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}
