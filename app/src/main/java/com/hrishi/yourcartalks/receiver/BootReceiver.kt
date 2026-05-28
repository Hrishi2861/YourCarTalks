package com.hrishi.yourcartalks.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hrishi.yourcartalks.GreetingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            GreetingService.start(context)
        }
    }
}
