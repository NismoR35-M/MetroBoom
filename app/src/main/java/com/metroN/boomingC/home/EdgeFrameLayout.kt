package com.metroN.boomingC.home

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import com.metroN.boomingC.util.systemBarInsetsCompat

/**
 * A [FrameLayout] that automatically applies bottom insets.
 */
class EdgeFrameLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
    init {
        clipToPadding = false
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Prevent excessive layouts by using translation instead of padding.
        translationY = -insets.systemBarInsetsCompat.bottom.toFloat()
        return insets
    }
}
