package org.snoozetown.ruhezeiten

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Schedules the two daily one-shot alarms (start/end of quiet hours) and reschedules
 * each one for the following day when it fires, since Android 12+ deprecated exact
 * repeating alarms in favor of self-rescheduling one-shot exact alarms.
 */
object AlarmScheduler {

    const val ACTION_START = "org.snoozetown.ruhezeiten.action.START_QUIET_HOURS"
    const val ACTION_END = "org.snoozetown.ruhezeiten.action.END_QUIET_HOURS"

    fun scheduleAll(context: Context) {
        val schedule = QuietHoursSchedule.load(context)
        cancelAll(context)
        if (!schedule.enabled) return

        scheduleNext(context, ACTION_START, schedule.startHour, schedule.startMinute)
        scheduleNext(context, ACTION_END, schedule.endHour, schedule.endMinute)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntentFor(context, ACTION_START))
        alarmManager.cancel(pendingIntentFor(context, ACTION_END))
    }

    /** Reschedules a single alarm for its next daily occurrence; called by the receiver after it fires. */
    fun rescheduleNext(context: Context, action: String) {
        val schedule = QuietHoursSchedule.load(context)
        if (!schedule.enabled) return
        val (hour, minute) = when (action) {
            ACTION_START -> schedule.startHour to schedule.startMinute
            ACTION_END -> schedule.endHour to schedule.endMinute
            else -> return
        }
        scheduleNext(context, action, hour, minute)
    }

    private fun scheduleNext(context: Context, action: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(hour, minute)
        val pendingIntent = pendingIntentFor(context, action)
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun pendingIntentFor(context: Context, action: String): PendingIntent {
        val intent = Intent(context, QuietHoursReceiver::class.java).setAction(action)
        val requestCode = if (action == ACTION_START) 1001 else 1002
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
