package com.metroN.boomingC.list

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A basic listener for list interactions.
 */
interface ClickableListListener<in T> {
    /**
     * Called when an item in the list is clicked.
     * @param item The [T] item that was clicked.
     * @param viewHolder The [RecyclerView.ViewHolder] of the item that was clicked.
     */
    fun onClick(item: T, viewHolder: RecyclerView.ViewHolder)

    /**
     * Binds this instance to a list item.
     * @param item The [T] to bind this item to.
     * @param viewHolder The [RecyclerView.ViewHolder] of the item that was clicked.
     * @param bodyView The [View] containing the main body of the list item. Any click events on
     * this [View] are routed to the listener. Defaults to the root view.
     */
    fun bind(item: T, viewHolder: RecyclerView.ViewHolder, bodyView: View = viewHolder.itemView) {
        bodyView.setOnClickListener { onClick(item, viewHolder) }
    }
}

/**
 * An extension of [ClickableListListener] that enables list editing functionality.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface EditableListListener<in T> : ClickableListListener<T> {
    /**
     * Called when a [RecyclerView.ViewHolder] requests that it should be dragged.
     * @param viewHolder The [RecyclerView.ViewHolder] that should start being dragged.
     */
    fun onPickUp(viewHolder: RecyclerView.ViewHolder)

    /**
     * Binds this instance to a list item.
     * @param item The [T] to bind this item to.
     * @param viewHolder The [RecyclerView.ViewHolder] to bind.
     * @param bodyView The [View] containing the main body of the list item. Any click events on
     * this [View] are routed to the listener. Defaults to the root view.
     * @param dragHandle A touchable [View]. Any drag on this view will start a drag event.
     */
    fun bind(
        item: T,
        viewHolder: RecyclerView.ViewHolder,
        bodyView: View = viewHolder.itemView,
        dragHandle: View
    ) {
        bind(item, viewHolder, bodyView)
        dragHandle.setOnTouchListener { _, motionEvent ->
            dragHandle.performClick()
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                onPickUp(viewHolder)
                true
            } else false
        }
    }
}

/**
 * An extension of [ClickableListListener] that enables menu and selection functionality.
 * @author Alexander Capehart (OxygenCobalt)
 */
interface SelectableListListener<in T> : ClickableListListener<T> {
    /**
     * Called when an item in the list requests that a menu related to it should be opened.
     * @param item The [T] item to open a menu for.
     * @param anchor The [View] to anchor the menu to.
     */
    fun onOpenMenu(item: T, anchor: View)

    /**
     * Called when an item in the list requests that it be selected.
     * @param item The [T] item to select.
     */
    fun onSelect(item: T)

    /**
     * Binds this instance to a list item.
     * @param item The [T] to bind this item to.
     * @param viewHolder The [RecyclerView.ViewHolder] to bind.
     * @param bodyView The [View] containing the main body of the list item. Any click events on
     * this [View] are routed to the listener. Defaults to the root view.
     * @param menuButton A clickable [View]. Any click events on this [View] will open a menu.
     */
    fun bind(
        item: T,
        viewHolder: RecyclerView.ViewHolder,
        bodyView: View = viewHolder.itemView,
        menuButton: View
    ) {
        bind(item, viewHolder, bodyView)
        // Map long clicks to the selection listener.
        bodyView.setOnLongClickListener {
            onSelect(item)
            true
        }
        // Map the menu button to the menu opening listener.
        menuButton.setOnClickListener { onOpenMenu(item, it) }
    }
}
