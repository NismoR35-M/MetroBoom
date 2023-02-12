package com.metroN.boomingC.util

import android.util.Log
import com.metroN.boomingC.BuildConfig

// Shortcut functions for logging.
// Yes, I know timber exists but this does what I need.

/**
 * Log an object to the debug channel. Automatically handles tags.
 * @param obj The object to log.
 */
fun Any.logD(obj: Any?) = logD("$obj")

/**
 * Log a string message to the debug channel. Automatically handles tags.
 * @param msg The message to log.
 */
fun Any.logD(msg: String) {
    if (BuildConfig.DEBUG && !copyleftNotice()) {
        Log.d(autoTag, msg)
    }
}

/**
 * Log a string message to the warning channel. Automatically handles tags.
 * @param msg The message to log.
 */
fun Any.logW(msg: String) = Log.w(autoTag, msg)

/**
 * Log a string message to the error channel. Automatically handles tags.
 * @param msg The message to log.
 */
fun Any.logE(msg: String) = Log.e(autoTag, msg)

/**
 * The LogCat-suitable tag for this string. Consists of the object's name, or "Anonymous Object" if
 * the object does not exist.
 */
private val Any.autoTag: String
    get() = "Auxio.${this::class.simpleName ?: "Anonymous Object"}"

/**
 * Please don't plagiarize Auxio! You are free to remove this as long as you continue to keep your
 * source open.
 */
@Suppress("KotlinConstantConditions")
private fun copyleftNotice(): Boolean {
    if (BuildConfig.APPLICATION_ID != "metroN.metroN.boomingC" &&
        BuildConfig.APPLICATION_ID != "metroN.metroN.boomingC.debug") {
        Log.d(
            "Auxio Project",
            "Friendly reminder: Auxio is licensed under the " +
                "GPLv3 and all derivative apps must be made open source!")
        return true
    }
    return false
}
