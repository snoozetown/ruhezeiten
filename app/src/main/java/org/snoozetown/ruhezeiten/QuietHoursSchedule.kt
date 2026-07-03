package org.snoozetown.ruhezeiten

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * DND level applied during quiet hours, mapped to NotificationManager.INTERRUPTION_FILTER_*.
 */
enum class DndLevel(val interruptionFilter: Int) {
    PRIORITY(NotificationManager.INTERRUPTION_FILTER_PRIORITY),
    ALARMS(NotificationManager.INTERRUPTION_FILTER_ALARMS),
    NONE(NotificationManager.INTERRUPTION_FILTER_NONE);
}

data class QuietHoursSchedule(
    val enabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val dndLevel: DndLevel
) {
    /** True if right now falls inside the scheduled window, handling overnight wraparound. */
    fun isCurrentlyActive(): Boolean {
        if (!enabled) return false
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute
        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    companion object {
        private const val PREFS_NAME = "ruhezeiten_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_START_HOUR = "start_hour"
        private const val KEY_START_MINUTE = "start_minute"
        private const val KEY_END_HOUR = "end_hour"
        private const val KEY_END_MINUTE = "end_minute"
        private const val KEY_DND_LEVEL = "dnd_level"

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun load(context: Context): QuietHoursSchedule {
            val p = prefs(context)
            return QuietHoursSchedule(
                enabled = p.getBoolean(KEY_ENABLED, false),
                startHour = p.getInt(KEY_START_HOUR, 22),
                startMinute = p.getInt(KEY_START_MINUTE, 0),
                endHour = p.getInt(KEY_END_HOUR, 7),
                endMinute = p.getInt(KEY_END_MINUTE, 0),
                dndLevel = DndLevel.valueOf(
                    p.getString(KEY_DND_LEVEL, DndLevel.PRIORITY.name) ?: DndLevel.PRIORITY.name
                )
            )
        }

        fun save(context: Context, schedule: QuietHoursSchedule) {
            prefs(context).edit()
                .putBoolean(KEY_ENABLED, schedule.enabled)
                .putInt(KEY_START_HOUR, schedule.startHour)
                .putInt(KEY_START_MINUTE, schedule.startMinute)
                .putInt(KEY_END_HOUR, schedule.endHour)
                .putInt(KEY_END_MINUTE, schedule.endMinute)
                .putString(KEY_DND_LEVEL, schedule.dndLevel.name)
                .apply()
        }
    }
}
