package com.metroN.boomingC.image

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.ImageViewCompat
import com.google.android.material.shape.MaterialShapeDrawable
import kotlin.math.max
import com.metroN.boomingC.R
import com.metroN.boomingC.ui.UISettings
import com.metroN.boomingC.util.getColorCompat
import com.metroN.boomingC.util.getDrawableCompat

/**
 * A view that displays an activation (i.e playback) indicator, with an accented styling and an
 * animated equalizer icon.
 *
 * This is only meant for use with [ImageGroup]. Due to limitations with [AnimationDrawable]
 * instances within custom views, this cannot be merged with [ImageGroup].
 */
class PlaybackIndicatorView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    AppCompatImageView(context, attrs, defStyleAttr) {
    private val playingIndicatorDrawable =
        context.getDrawableCompat(R.drawable.ic_playing_indicator_24) as AnimationDrawable
    private val pausedIndicatorDrawable =
        context.getDrawableCompat(R.drawable.ic_paused_indicator_24)
    private val indicatorMatrix = Matrix()
    private val indicatorMatrixSrc = RectF()
    private val indicatorMatrixDst = RectF()

    /**
     * The corner radius of this view. This allows the outer ImageGroup to apply it's corner radius
     * to this view without any attribute hacks.
     */
    var cornerRadius = 0f
        set(value) {
            field = value
            (background as? MaterialShapeDrawable)?.let { bg ->
                if (UISettings.from(context).roundMode) {
                    bg.setCornerSize(value)
                } else {
                    bg.setCornerSize(0f)
                }
            }
        }

    /**
     * Whether this view should be indicated to have ongoing playback or not. If true, the animated
     * playing icon will be shown. If false, the static paused icon will be shown.
     */
    var isPlaying: Boolean
        get() = drawable == playingIndicatorDrawable
        set(value) {
            if (value) {
                playingIndicatorDrawable.start()
                setImageDrawable(playingIndicatorDrawable)
            } else {
                playingIndicatorDrawable.stop()
                setImageDrawable(pausedIndicatorDrawable)
            }
        }

    init {
        // We will need to manually re-scale the playing/paused drawables to align with
        // StyledDrawable, so use the matrix scale type.
        scaleType = ScaleType.MATRIX
        // Tint the playing/paused drawables so they are harmonious with the background.
        ImageViewCompat.setImageTintList(this, context.getColorCompat(R.color.sel_on_cover_bg))

        // Use clipToOutline and a background drawable to crop images. While Coil's transformation
        // could theoretically be used to round corners, the corner radius is dependent on the
        // dimensions of the image, which will result in inconsistent corners across different
        // album covers unless we resize all covers to be the same size. clipToOutline is both
        // cheaper and more elegant. As a side-note, this also allows us to re-use the same
        // background for both the tonal background color and the corner rounding.
        clipToOutline = true
        background =
            MaterialShapeDrawable().apply {
                fillColor = context.getColorCompat(R.color.sel_cover_bg)
                setCornerSize(cornerRadius)
            }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Emulate StyledDrawable scaling with matrix scaling.
        val iconSize = max(measuredWidth, measuredHeight) / 2
        imageMatrix =
            indicatorMatrix.apply {
                reset()
                drawable?.let { drawable ->
                    // First scale the icon up to the desired size.
                    indicatorMatrixSrc.set(
                        0f,
                        0f,
                        drawable.intrinsicWidth.toFloat(),
                        drawable.intrinsicHeight.toFloat())
                    indicatorMatrixDst.set(0f, 0f, iconSize.toFloat(), iconSize.toFloat())
                    indicatorMatrix.setRectToRect(
                        indicatorMatrixSrc, indicatorMatrixDst, Matrix.ScaleToFit.CENTER)

                    // Then actually center it into the icon.
                    indicatorMatrix.postTranslate(
                        (measuredWidth - iconSize) / 2f, (measuredHeight - iconSize) / 2f)
                }
            }
    }
}
