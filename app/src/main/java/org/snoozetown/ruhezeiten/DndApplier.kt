package org.snoozetown.ruhezeiten

import android.app.NotificationManager
import android.content.Context

/**
 * Applies or clears DND state for a given level. Shared between QuietHoursReceiver (the
 * scheduled start/end alarms) and the Save button (so an edit made while already inside
 * an active quiet-hours window takes effect immediately instead of waiting for the next
 * scheduled boundary).
 */
object DndApplier {

    fun applyStart(context: Context, dndLevel: DndLevel) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        if (dndLevel == DndLevel.PRIORITY) {
            // INTERRUPTION_FILTER_PRIORITY on its own just defers to whatever the device's
            // own Priority-category settings happen to be -- on the Kompakt those are only
            // reachable through a Settings screen that hard-crashes
            // (ZenModeAlarmsPreferenceController ClassCastException), so there's no way for
            // the user to fix a misconfigured category from the OS side. Setting an explicit
            // policy here guarantees "alarms, plus starred contacts" is actually true
            // regardless of that broken screen -- at the cost of overwriting whatever
            // system-wide DND category policy exists, not just for this app's window.
            notificationManager.setNotificationPolicy(
                NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                    NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY
                )
            )
        }

        notificationManager.setInterruptionFilter(dndLevel.interruptionFilter)
    }

    fun applyEnd(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) return
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
}
