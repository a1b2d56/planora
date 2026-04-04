package com.devil.taskzio.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager

@Suppress("unused")
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule reminders on device boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SavingsReminderWorker.schedule(WorkManager.getInstance(context))
        }
    }
}
