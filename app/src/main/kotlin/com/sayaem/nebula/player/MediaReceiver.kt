package com.sayaem.nebula.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Broadcast to any active PlayerController via a global event bus
        val localIntent = Intent(intent.action).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(localIntent)
    }
}
