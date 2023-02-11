package com.metroN.boomingC.playback.system

import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * A [BroadcastReceiver] that starts music playback when a bluetooth headset is connected.
 */
class BluetoothHeadsetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val newState =
                intent.getIntExtra(
                    BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // TODO: Initialize the service (Permission workflow must be figured out)
                //  Perhaps move this to the internal receivers?
            }
        }
    }
}
