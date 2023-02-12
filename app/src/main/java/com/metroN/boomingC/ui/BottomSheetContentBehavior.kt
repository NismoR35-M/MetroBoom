package com.metroN.boomingC.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BackportBottomSheetBehavior
import com.metroN.boomingC.util.coordinatorLayoutBehavior
import com.metroN.boomingC.util.replaceSystemBarInsetsCompat
import com.metroN.boomingC.util.systemBarInsetsCompat
import kotlin.math.abs

class BottomSheetContentBehavior<V : View>(context: Context, attributeSet: AttributeSet?) :
    CoordinatorLayout.Behavior<V>(context, attributeSet) {
    private var dep: View? = null
    private var lastInsets: WindowInsets? = null
    private var lastConsumed = -1
    private var setup = false

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean {
        if (dependency.coordinatorLayoutBehavior is BackportBottomSheetBehavior) {
            dep = dependency
            return true
        }

        return false
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: V,
        dependency: View
    ): Boolean {
        val behavior = dependency.coordinatorLayoutBehavior as BackportBottomSheetBehavior
        val consumed = behavior.calculateConsumedByBar()
        if (consumed == Int.MIN_VALUE) {
            return false
        }

        if (consumed != lastConsumed) {
            lastConsumed = consumed

            val insets = lastInsets
            if (insets != null) {
                child.dispatchApplyWindowInsets(insets)
            }

            lastInsets?.let(child::dispatchApplyWindowInsets)
            measureContent(parent, child, consumed)
            layoutContent(child)
            return true
        }

        return false
    }

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: V,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        val dep = dep ?: return false
        val behavior = dep.coordinatorLayoutBehavior as BackportBottomSheetBehavior
        val consumed = behavior.calculateConsumedByBar()
        if (consumed == Int.MIN_VALUE) {
            return false
        }

        measureContent(parent, child, consumed)

        return true
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: V, layoutDirection: Int): Boolean {
        super.onLayoutChild(parent, child, layoutDirection)
        layoutContent(child)

        if (!setup) {
            child.setOnApplyWindowInsetsListener { _, insets ->
                lastInsets = insets
                val dep = dep ?: return@setOnApplyWindowInsetsListener insets
                val behavior = dep.coordinatorLayoutBehavior as BackportBottomSheetBehavior
                val consumed = behavior.calculateConsumedByBar()
                if (consumed == Int.MIN_VALUE) {
                    return@setOnApplyWindowInsetsListener insets
                }

                val bars = insets.systemBarInsetsCompat

                insets.replaceSystemBarInsetsCompat(
                    bars.left, bars.top, bars.right, (bars.bottom - consumed).coerceAtLeast(0))
            }

            setup = true
        }

        return true
    }

    private fun measureContent(parent: View, child: View, consumed: Int) {
        val contentWidthSpec =
            View.MeasureSpec.makeMeasureSpec(parent.measuredWidth, View.MeasureSpec.EXACTLY)
        val contentHeightSpec =
            View.MeasureSpec.makeMeasureSpec(
                parent.measuredHeight - consumed, View.MeasureSpec.EXACTLY)

        child.measure(contentWidthSpec, contentHeightSpec)
    }

    private fun layoutContent(child: View) {
        child.layout(0, 0, child.measuredWidth, child.measuredHeight)
    }

    private fun BackportBottomSheetBehavior<*>.calculateConsumedByBar(): Int {
        val offset = calculateSlideOffset()
        if (offset == Float.MIN_VALUE || peekHeight < 0) {
            return Int.MIN_VALUE
        }

        return if (offset >= 0) {
            peekHeight
        } else {
            (peekHeight * (1 - abs(offset))).toInt()
        }
    }
}
