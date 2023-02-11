package com.metroN.boomingC.playback.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.metroN.boomingC.R
import com.metroN.boomingC.util.getInteger

/**
 * A [MaterialButton] that automatically morphs from a circle to a squircle shape appearance when
 * [isActivated] changes.
 */
class AnimatedMaterialButton
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    MaterialButton(context, attrs, defStyleAttr) {
    private var currentCornerRadiusRatio = 0f
    private var animator: ValueAnimator? = null

    override fun setActivated(activated: Boolean) {
        super.setActivated(activated)

        // Activated -> Squircle (30% Radius), Inactive -> Circle (50% Radius)
        val targetRadius = if (activated) 0.3f else 0.5f
        if (!isLaidOut) {
            // Not laid out, initialize it without animation before drawing.
            updateCornerRadiusRatio(targetRadius)
            return
        }

        animator?.cancel()
        animator =
            ValueAnimator.ofFloat(currentCornerRadiusRatio, targetRadius).apply {
                duration = context.getInteger(R.integer.anim_fade_enter_duration).toLong()
                addUpdateListener { updateCornerRadiusRatio(animatedValue as Float) }
                start()
            }
    }

    private fun updateCornerRadiusRatio(ratio: Float) {
        currentCornerRadiusRatio = ratio
        // Can't reproduce the intrinsic ratio corner radius, just manually implement it with
        // a dimension value.
        shapeAppearanceModel = shapeAppearanceModel.withCornerSize { it.width() * ratio }
    }
}
