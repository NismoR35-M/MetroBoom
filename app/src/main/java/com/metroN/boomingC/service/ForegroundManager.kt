
 
package com.metroN.boomingC.service

import android.app.Service
import androidx.core.app.ServiceCompat
import com.metroN.boomingC.util.logD

/**
 * A utility to create consistent foreground behavior for a given [Service].
 * @param service [Service] to wrap in this instance.
 *
 * TODO: Merge with unified service when done.
 */
class ForegroundManager(private val service: Service) {
    private var isForeground = false

    /** Release this instance. */
    fun release() {
        tryStopForeground()
    }

    /**
     * Try to enter a foreground state.
     * @param notification The [ForegroundServiceNotification] to show in order to signal the
     * foreground state.
     * @return true if the state was changed, false otherwise
     * @see Service.startForeground
     */
    fun tryStartForeground(notification: ForegroundServiceNotification): Boolean {
        if (isForeground) {
            // Nothing to do.
            return false
        }

        logD("Starting foreground state")
        service.startForeground(notification.code, notification.build())
        isForeground = true
        return true
    }

    /**
     * Try to exit a foreground state. Will remove the foreground notification.
     * @return true if the state was changed, false otherwise
     * @see Service.stopForeground
     */
    fun tryStopForeground(): Boolean {
        if (!isForeground) {
            // Nothing to do.
            return false
        }

        logD("Stopping foreground state")
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        return true
    }
}
