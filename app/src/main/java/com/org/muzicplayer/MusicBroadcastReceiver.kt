package com.org.muzicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = intent?.action
        }
        context?.startService(serviceIntent)
    }
}
