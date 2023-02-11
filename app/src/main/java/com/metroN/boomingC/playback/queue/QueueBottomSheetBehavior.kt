package com.metroN.boomingC.playback.queue

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.shape.MaterialShapeDrawable
import com.metroN.boomingC.R
import com.metroN.boomingC.ui.BaseBottomSheetBehavior
import com.metroN.boomingC.util.getAttrColorCompat
import com.metroN.boomingC.util.getDimen
import com.metroN.boomingC.util.getDimenPixels
import com.metroN.boomingC.util.replaceSystemBarInsetsCompat
import com.metroN.boomingC.util.systemBarInsetsCompat

/**
 * The [BaseBottomSheetBehavior] for the queue bottom sheet. This is placed within the playback
 * sheet and automatically arranges itself to show the playback bar at the top.*/
class QueueBottomSheetBehavior<V : View>(context: Context, attributeSet: AttributeSet?) :
    BaseBottomSheetBehavior<V>(context, attributeSet) {
    private var barHeight = 0
    private var barSpacing = context.getDimenPixels(R.dimen.spacing_small)

    init {
        // Not hide-able (and not programmatically hide-able)
        isHideable = false
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View) =
        dependency.id == R.id.playback_bar_fragment

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: V,
        dependency: View
    ): Boolean {
        barHeight = dependency.height
        // No change, just grabbed the height
        return false
    }

    override fun createBackground(context: Context) =
        MaterialShapeDrawable.createWithElevationOverlay(context).apply {
            // The queue sheet's background is a static elevated background.
            fillColor = context.getAttrColorCompat(R.attr.colorSurface)
            elevation = context.getDimen(R.dimen.elevation_normal)
        }

    override fun applyWindowInsets(child: View, insets: WindowInsets): WindowInsets {
        super.applyWindowInsets(child, insets)
        // Offset our expanded panel by the size of the playback bar, as that is shown when
        // we slide up the panel.
        val bars = insets.systemBarInsetsCompat
        expandedOffset = bars.top + barHeight + barSpacing
        return insets.replaceSystemBarInsetsCompat(
            bars.left, bars.top, bars.right, expandedOffset + bars.bottom)
    }
}
