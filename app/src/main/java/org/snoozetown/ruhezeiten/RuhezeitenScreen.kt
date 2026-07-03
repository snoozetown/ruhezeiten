package org.snoozetown.ruhezeiten

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.radio_button.RadioButtonMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.time.TimeInputMMD
import com.mudita.mmd.components.time.rememberTimeInputMMDState
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuhezeitenScreen() {
    val context = LocalContext.current
    val schedule = remember { mutableStateOf(QuietHoursSchedule.load(context)) }

    var enabled by remember { mutableStateOf(schedule.value.enabled) }
    var dndLevel by remember { mutableStateOf(schedule.value.dndLevel) }
    var dndGranted by remember { mutableStateOf(isDndAccessGranted(context)) }
    var alarmGranted by remember { mutableStateOf(isExactAlarmGranted(context)) }
    var batteryUnrestricted by remember { mutableStateOf(isBatteryUnrestricted(context)) }
    var justSaved by remember { mutableStateOf(false) }

    val startTimeState = rememberTimeInputMMDState(
        initialHour = schedule.value.startHour,
        initialMinute = schedule.value.startMinute,
        is24Hour = true
    )
    val endTimeState = rememberTimeInputMMDState(
        initialHour = schedule.value.endHour,
        initialMinute = schedule.value.endMinute,
        is24Hour = true
    )

    // Re-check permission state whenever the user returns from a system settings screen.
    LifecycleResumeEffect(Unit) {
        dndGranted = isDndAccessGranted(context)
        alarmGranted = isExactAlarmGranted(context)
        batteryUnrestricted = isBatteryUnrestricted(context)
        onPauseOrDispose { }
    }

    // Each TimeInputMMD autofocuses its own hour field on first composition (Material3
    // TimeInput's default, meant for modal use), brings itself into view, and pops the
    // keyboard. With two TimeInputMMD instances on screen, the second one's autofocus wins
    // that race and scrolls the whole screen down to reveal itself, hiding everything above
    // it (permission section, switch, first time input) behind the top app bar. A single
    // clear-and-hide after a fixed delay isn't reliable -- on a slow/cold launch the actual
    // autofocus can fire *after* that one-shot check already ran, leaving the keyboard open
    // with nothing left to dismiss it. Instead, keep clearing focus and hiding the keyboard
    // for a short settling window after launch, so whichever field ends up winning the
    // autofocus race gets caught regardless of exactly when it fires -- then stop, so a
    // deliberate tap on a time field later in the session opens the keyboard normally.
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        repeat(15) {
            delay(100)
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
        scrollState.scrollTo(0)
    }

    // Any actual edit invalidates the last save's confirmation message.
    LaunchedEffect(enabled, dndLevel, startTimeState.hour, startTimeState.minute, endTimeState.hour, endTimeState.minute) {
        justSaved = false
    }

    Scaffold(
        topBar = {
            TopAppBarMMD(title = { TextMMD(stringResource(R.string.app_name)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Permissions come first — nothing else on this screen works without them, and
            // each item (label, status, button) disappears entirely once granted rather than
            // leaving a permanent "Granted" line with nothing left to do about it. The whole
            // section vanishes once both are granted; LifecycleResumeEffect above brings it
            // back automatically if a permission is ever later revoked.
            if (!dndGranted || !alarmGranted || !batteryUnrestricted) {
                TextMMD(
                    text = stringResource(R.string.permission_section_title),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                )
                Spacer(12.dp)

                if (!dndGranted) {
                    TextMMD(stringResource(R.string.dnd_permission_label))
                    TextMMD(stringResource(R.string.dnd_permission_not_granted))
                    Spacer(4.dp)
                    OutlinedButtonMMD(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextMMD(stringResource(R.string.dnd_permission_button))
                    }
                    Spacer(16.dp)
                }

                if (!alarmGranted) {
                    TextMMD(stringResource(R.string.alarm_permission_label))
                    TextMMD(stringResource(R.string.alarm_permission_not_granted))
                    Spacer(4.dp)
                    OutlinedButtonMMD(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}"))
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextMMD(stringResource(R.string.alarm_permission_button))
                    }
                    Spacer(16.dp)
                }

                if (!batteryUnrestricted) {
                    TextMMD(stringResource(R.string.battery_permission_label))
                    TextMMD(stringResource(R.string.battery_permission_not_granted))
                    Spacer(4.dp)
                    OutlinedButtonMMD(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextMMD(stringResource(R.string.battery_permission_button))
                    }
                }

                Divider20()
            }

            // Reflects the saved/active schedule, not live edits below — those aren't in
            // effect until Save is tapped, so this must not change until then either.
            TextMMD(
                text = if (schedule.value.enabled) {
                    stringResource(
                        R.string.status_active,
                        formatTime(schedule.value.startHour, schedule.value.startMinute),
                        formatTime(schedule.value.endHour, schedule.value.endMinute)
                    )
                } else {
                    stringResource(R.string.status_inactive)
                },
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Spacer(20.dp)

            // The Row is the single toggleable target (merging the visible label as its
            // accessible name); SwitchMMD's own onCheckedChange is left null so it's purely
            // visual and doesn't create a second, unlabeled TalkBack stop.
            val enableLabel = stringResource(R.string.enable_switch_label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                        value = enabled,
                        onValueChange = { enabled = it },
                        role = Role.Switch
                    )
                    .semantics { contentDescription = enableLabel },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                TextMMD(enableLabel, modifier = Modifier.clearAndSetSemantics {})
                SwitchMMD(
                    checked = enabled,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }

            Divider20()

            TextMMD(stringResource(R.string.start_time_label))
            Spacer(8.dp)
            TimeInputMMD(state = startTimeState)

            Spacer(20.dp)

            TextMMD(stringResource(R.string.end_time_label))
            Spacer(8.dp)
            TimeInputMMD(state = endTimeState)

            Divider20()

            TextMMD(stringResource(R.string.dnd_level_label))
            Spacer(8.dp)
            Column(Modifier.selectableGroup()) {
                DndLevelOption(
                    label = stringResource(R.string.dnd_level_priority),
                    selected = dndLevel == DndLevel.PRIORITY,
                    onSelect = { dndLevel = DndLevel.PRIORITY }
                )
                DndLevelOption(
                    label = stringResource(R.string.dnd_level_alarms),
                    selected = dndLevel == DndLevel.ALARMS,
                    onSelect = { dndLevel = DndLevel.ALARMS }
                )
                DndLevelOption(
                    label = stringResource(R.string.dnd_level_none),
                    selected = dndLevel == DndLevel.NONE,
                    onSelect = { dndLevel = DndLevel.NONE }
                )
            }

            val hasPendingChanges = enabled != schedule.value.enabled ||
                dndLevel != schedule.value.dndLevel ||
                startTimeState.hour != schedule.value.startHour ||
                startTimeState.minute != schedule.value.startMinute ||
                endTimeState.hour != schedule.value.endHour ||
                endTimeState.minute != schedule.value.endMinute

            if (hasPendingChanges) {
                Divider20()

                // A preview of exactly what Save will apply, shown right next to the button —
                // scrolling to the bottom to tap Save shouldn't require also remembering or
                // scrolling back up to check what you set earlier. Only shown when there's
                // actually something unsaved to preview.
                TextMMD(
                    text = stringResource(R.string.save_preview_title),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                )
                Spacer(8.dp)
                TextMMD(
                    stringResource(
                        if (enabled) R.string.save_preview_enabled else R.string.save_preview_disabled
                    )
                )
                if (enabled) {
                    TextMMD(
                        stringResource(
                            R.string.save_preview_times,
                            formatTime(startTimeState.hour, startTimeState.minute),
                            formatTime(endTimeState.hour, endTimeState.minute)
                        )
                    )
                    TextMMD(stringResource(R.string.save_preview_level, dndLevelShortLabel(dndLevel)))
                }

                Spacer(16.dp)
            } else {
                Divider20()
            }

            ButtonMMD(
                onClick = {
                    val updated = QuietHoursSchedule(
                        enabled = enabled,
                        startHour = startTimeState.hour,
                        startMinute = startTimeState.minute,
                        endHour = endTimeState.hour,
                        endMinute = endTimeState.minute,
                        dndLevel = dndLevel
                    )
                    QuietHoursSchedule.save(context, updated)
                    AlarmScheduler.scheduleAll(context)
                    // Reflect the edit in the live DND state immediately if we're currently
                    // inside (or just left) the scheduled window -- otherwise an edit made
                    // mid-session would silently do nothing until the next scheduled
                    // boundary, since the alarms above only affect the *next* start/end.
                    if (updated.isCurrentlyActive()) {
                        DndApplier.applyStart(context, updated.dndLevel)
                    } else {
                        DndApplier.applyEnd(context)
                    }
                    schedule.value = updated
                    justSaved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextMMD(stringResource(R.string.save_button))
            }

            if (justSaved) {
                Spacer(8.dp)
                TextMMD(
                    text = stringResource(R.string.save_confirmation),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            Spacer(20.dp)
        }
    }
}

@Composable
private fun DndLevelOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    // A single interactive target on the Row; RadioButtonMMD's own onClick is left null so it's
    // purely visual, avoiding nested clickables (which TalkBack would announce twice per row).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = label }
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButtonMMD(selected = selected, onClick = null, modifier = Modifier.clearAndSetSemantics {})
        Spacer(8.dp, horizontal = true)
        TextMMD(label, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun Spacer(size: androidx.compose.ui.unit.Dp, horizontal: Boolean = false) {
    if (horizontal) {
        androidx.compose.foundation.layout.Spacer(Modifier.width(size))
    } else {
        androidx.compose.foundation.layout.Spacer(Modifier.height(size))
    }
}

@Composable
private fun dndLevelShortLabel(level: DndLevel): String = stringResource(
    when (level) {
        DndLevel.PRIORITY -> R.string.dnd_level_priority_short
        DndLevel.ALARMS -> R.string.dnd_level_alarms_short
        DndLevel.NONE -> R.string.dnd_level_none_short
    }
)

@Composable
private fun Divider20() {
    Spacer(16.dp)
    androidx.compose.material3.HorizontalDivider()
    Spacer(16.dp)
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

private fun isDndAccessGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.isNotificationPolicyAccessGranted
}

private fun isExactAlarmGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return am.canScheduleExactAlarms()
}

private fun isBatteryUnrestricted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
