# Changelog

All notable changes to this project are documented here. Format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.1.0] - 2026-07-03

### Added

- Battery optimization permission item, alongside DND access and exact alarm
  scheduling -- with its own button and lifecycle recheck.

### Changed

- Priority mode now sets its own notification policy (alarms, plus starred
  contacts) directly, instead of relying on the device's own Priority-category
  settings -- those are reachable only through a Kompakt Settings screen that
  crashes.
- Saving a schedule change now applies immediately if you're currently inside
  (or just left) the scheduled window, instead of waiting for the next
  scheduled start/end alarm to fire.
- Install instructions now lead with Mudita Center (with ADB as an
  alternative), and the README documents a workaround for DuraSpeed, a
  MediaTek background-app killer that can delay quiet hours starting or
  ending by up to an hour.

### Fixed

- Keyboard no longer pops up automatically when opening the app.

## [1.0.0] - 2026-07-02

### Added

- Initial release: scheduled quiet hours with a configurable start time, end
  time, and DND level (Priority only / Alarms only / Total silence).
- Self-rescheduling exact alarms; survives reboot.
- Permission flow for Do Not Disturb access and exact alarm scheduling.
- UI built on Mudita Mindful Design (MMD), matching MuditaOS K's e-ink design
  system.
