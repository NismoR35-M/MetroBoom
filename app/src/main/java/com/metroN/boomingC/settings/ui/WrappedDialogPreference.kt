package com.metroN.boomingC.settings.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

/**
 * Wraps a [DialogPreference] to be instantiatable. This has no purpose other to ensure that custom
 * dialog preferences are handled.
 */
class WrappedDialogPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.dialogPreferenceStyle,
    defStyleRes: Int = 0
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes)
