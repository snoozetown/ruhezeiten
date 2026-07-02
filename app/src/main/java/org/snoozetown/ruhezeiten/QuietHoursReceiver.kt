package org.snoozetown.ruhezeiten

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class QuietHoursReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            // Permission was revoked after scheduling; nothing we can do until re-granted.
            AlarmScheduler.rescheduleNext(context, intent.action ?: return)
            return
        }

        when (intent.action) {
            AlarmScheduler.ACTION_START -> {
                val schedule = QuietHoursSchedule.load(context)
                notificationManager.setInterruptionFilter(schedule.dndLevel.interruptionFilter)
            }
            AlarmScheduler.ACTION_END -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }

        AlarmScheduler.rescheduleNext(context, intent.action ?: return)
    }
}
