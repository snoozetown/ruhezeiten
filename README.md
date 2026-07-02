# Ruhezeiten

Scheduled quiet hours for the Mudita Kompakt.

MuditaOS K's own Do Not Disturb screen crashes when you try to open its "People" or
"Alarms & other interruptions" settings (a `ClassCastException` in the underlying
Settings app), and as of this writing there's no scheduling UI for DND at all. Ruhezeiten
is a small standalone app that works around both problems: it toggles Android's real,
system-level Do Not Disturb state on a daily schedule you set, without touching the
broken screens.

## What it does

- Set a daily start time and end time for quiet hours (24-hour format).
- Choose what's still allowed through: Priority only (starred contacts/alarms), Alarms
  only, or total silence.
- Runs as two self-rescheduling exact alarms — no foreground service, no battery
  drain beyond what any alarm-based app costs.
- Survives reboot.

## What it does *not* do

- It does not touch or fix MuditaOS K's own Settings app — that's Mudita's own crash
  to fix. This just gives you the feature that screen is supposed to provide.
- It does not manage contacts or calendars. "Priority only" reflects whatever contacts
  you've starred in Mudita's own Contacts, Dialer, or Messages apps (verified: starring
  in any of the three correctly updates the standard Android contacts database that
  this app — and Android's DND filter itself — reads from). If your authoritative
  contacts live somewhere that doesn't sync onto the device (e.g. CardDAV providers
  that don't currently work well with MuditaOS K), that's outside this app's scope.

## Installing

No root required — this is a normal Android app, installed the same way as any
sideloaded APK on Kompakt (USB-C cable + ADB, per Mudita's own sideloading support).

1. Download the latest APK from [Releases](../../releases).
2. Enable USB debugging on the Kompakt and connect it to a computer.
3. `adb install ruhezeiten.apk`

## Setup

After installing, open Ruhezeiten and grant the two permissions it asks for:

- **Do Not Disturb access** — required for the app to change DND state at all.
- **Exact alarm scheduling** — required so quiet hours start/end on time rather than
  drifting by minutes to hours under Android's Doze battery optimizations.

Both are standard Android permission screens (not the broken MuditaOS K ones), opened
directly by the app.

## Building from source

Requires JDK 17+, the Android SDK (`compileSdk 35`, `minSdk`/`targetSdk 31` to match
MuditaOS K's Android 12 base), and Gradle (a wrapper is included).

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Built with

UI is built on [Mudita Mindful Design (MMD)](https://github.com/mudita/MMD), Mudita's
own e-ink optimized Jetpack Compose component library — pure monochrome color scheme,
no ripple/animation effects, large touch targets. See [NOTICE.md](NOTICE.md) for
attribution details.

## License

GPLv3 — see [LICENSE](LICENSE). Contributions and forks are welcome; derivatives must
stay open source under the same terms.

## Contact

Bugs and feature requests: please use [Issues](../../issues). For anything else,
public@snoozetown.org.
