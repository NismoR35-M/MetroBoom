
package com.metroN.boomingC.music.system

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.metroN.boomingC.BuildConfig
import com.metroN.boomingC.IntegerTable
import com.metroN.boomingC.R
import com.metroN.boomingC.service.ForegroundServiceNotification
import com.metroN.boomingC.util.logD
import com.metroN.boomingC.util.newMainPendingIntent

/**
 * A dynamic [ForegroundServiceNotification] that shows the current music loading state.
 * @param context [Context] required to create the notification.
 */
class IndexingNotification(private val context: Context) :
    ForegroundServiceNotification(context, INDEXER_CHANNEL) {
    private var lastUpdateTime = -1L

    init {
        setSmallIcon(R.drawable.ic_indexer_24)
        setCategory(NotificationCompat.CATEGORY_PROGRESS)
        setShowWhen(false)
        setSilent(true)
        setContentIntent(context.newMainPendingIntent())
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.getString(R.string.lbl_indexing))
        setContentText(context.getString(R.string.lng_indexing))
        setProgress(0, 0, true)
    }

    override val code: Int
        get() = IntegerTable.INDEXER_NOTIFICATION_CODE

    /**
     * Update this notification with the new music loading state.
     * @param indexing The new music loading state to display in the notification.
     * @return true if the notification updated, false otherwise
     */
    fun updateIndexingState(indexing: Indexer.Indexing): Boolean {
        when (indexing) {
            is Indexer.Indexing.Indeterminate -> {
                // Indeterminate state, use a vaguer description and in-determinate progress.
                // These events are not very frequent, and thus we don't need to safeguard
                // against rate limiting.
                logD("Updating state to $indexing")
                lastUpdateTime = -1
                setContentText(context.getString(R.string.lng_indexing))
                setProgress(0, 0, true)
                return true
            }
            is Indexer.Indexing.Songs -> {
                // Determinate state, show an active progress meter. Since these updates arrive
                // highly rapidly, only update every 1.5 seconds to prevent notification rate
                // limiting.
                val now = SystemClock.elapsedRealtime()
                if (lastUpdateTime > -1 && (now - lastUpdateTime) < 1500) {
                    return false
                }
                lastUpdateTime = SystemClock.elapsedRealtime()
                logD("Updating state to $indexing")
                setContentText(
                    context.getString(R.string.fmt_indexing, indexing.current, indexing.total))
                setProgress(indexing.total, indexing.current, false)
                return true
            }
        }
    }
}

/**
 * A static [ForegroundServiceNotification] that signals to the user that the app is currently
 * monitoring the music library for changes.
 * @author Alexander Capehart (OxygenCobalt)
 */
class ObservingNotification(context: Context) :
    ForegroundServiceNotification(context, INDEXER_CHANNEL) {
    init {
        setSmallIcon(R.drawable.ic_indexer_24)
        setCategory(NotificationCompat.CATEGORY_SERVICE)
        setShowWhen(false)
        setSilent(true)
        setContentIntent(context.newMainPendingIntent())
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.getString(R.string.lbl_observing))
        setContentText(context.getString(R.string.lng_observing))
    }

    override val code: Int
        get() = IntegerTable.INDEXER_NOTIFICATION_CODE
}

/** Notification channel shared by [IndexingNotification] and [ObservingNotification]. */
private val INDEXER_CHANNEL =
    ForegroundServiceNotification.ChannelInfo(
        id = BuildConfig.APPLICATION_ID + ".channel.INDEXER", nameRes = R.string.lbl_indexer)
