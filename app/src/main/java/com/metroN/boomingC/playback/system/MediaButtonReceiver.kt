package com.metroN.boomingC.playback.system

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.metroN.boomingC.playback.state.PlaybackStateManager

/**
 * A [BroadcastReceiver] that forwards [Intent.ACTION_MEDIA_BUTTON] [Intent]s to [PlaybackService].
 */
class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val playbackManager = PlaybackStateManager.get()
        if (playbackManager.queue.currentSong != null) {
            // We have a song, so we can assume that the service will start a foreground state.
            // At least, I hope. Again, *this is why we don't do this*. I cannot describe how
            // stupid this is with the state of foreground services on modern android. One
            // wrong action at the wrong time will result in the app crashing, and there is
            // nothing I can do about it.
            intent.component = ComponentName(context, PlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
