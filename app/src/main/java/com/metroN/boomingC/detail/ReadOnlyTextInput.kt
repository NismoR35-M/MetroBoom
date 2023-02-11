package com.metroN.boomingC.detail

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.metroN.boomingC.R

/**
 * A [TextInputEditText] that deliberately restricts all input except for selection. This will work
 * just like a normal block of selectable/copyable text, but with nicer aesthetics.
 *
 * Adapted from Material Files: https://github.com/zhanghai/MaterialFiles
 */
class ReadOnlyTextInput
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) : TextInputEditText(context, attrs, defStyleAttr) {
    init {
        // Enable selection, but still disable focus (i.e Keyboard opening)
        setTextIsSelectable(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusable = View.FOCUSABLE_AUTO
        }
    }

    // Make text immutable
    override fun getFreezesText() = false
    // Prevent editing by default
    override fun getDefaultEditable() = false
    // Remove the movement method that allows cursor scrolling
    override fun getDefaultMovementMethod() = null
}
