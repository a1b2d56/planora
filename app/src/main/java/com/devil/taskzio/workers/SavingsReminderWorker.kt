package com.devil.taskzio.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.devil.taskzio.R
import com.devil.taskzio.data.repository.SavingsGoalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit

@HiltWorker
class SavingsReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SavingsGoalRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val goals = repository.getActiveGoals().first()
            .filter { it.reminderEnabled && !it.isCompleted }
        if (goals.isEmpty()) return Result.success()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Savings Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Daily reminders to save towards your goals" }
        )

        goals.forEachIndexed { index, goal ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_savings)   // branded icon, not system drawable
                .setContentTitle("Daily Saving Reminder")
                .setContentText(
                    "Save ${com.devil.taskzio.utils.FormatUtils.formatCurrency(goal.dailySaving)} today for \"${goal.title}\""
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(NOTIFICATION_ID_BASE + index, notification)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID           = "savings_reminder_channel"
        const val NOTIFICATION_ID_BASE = 1000
        const val WORK_NAME            = "savings_daily_reminder"

        fun schedule(workManager: WorkManager) {
            // Calculate delay to next 9:00 AM — so reminders always fire at a sensible time
            val zone = ZoneId.systemDefault()
            val now  = ZonedDateTime.now(zone)
            var next9AM = now.toLocalDate().atTime(9, 0).atZone(zone)
            if (!next9AM.isAfter(now)) next9AM = next9AM.plusDays(1)
            val delayMs = Duration.between(now, next9AM).toMillis()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SavingsReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .build()
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
