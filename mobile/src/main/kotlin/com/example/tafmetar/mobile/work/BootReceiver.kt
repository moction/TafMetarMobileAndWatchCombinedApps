package com.example.tafmetar.mobile.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-planifie le WorkManager après un redémarrage du téléphone
 * (les tâches périodiques ne survivent pas nativement à un reboot sur certains OEM).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkScheduler.schedulePeriodicRefresh(context)
        }
    }
}
