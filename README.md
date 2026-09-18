# MemWatch

A small Android app that answers "why is 1 GB of my RAM in use when nothing is open?"
and gets you to Android's own **Force stop** button in one tap.

## What it shows

- **Real RAM numbers** from `ActivityManager.getMemoryInfo()` and `/proc/meminfo`:
  a stacked bar of in use / reclaimable cache / free, available, swap (zRAM) and the
  kernel's low-memory threshold, plus a Healthy / Getting tight / Low memory badge.
  MemWatch's own footprint is listed separately as "MemWatch itself (PSS)".
- **Per-app RAM, via Android.** Since Android 8 (and stricter on 10+) an ordinary app
  cannot read other apps' RAM, so MemWatch does not pretend to. Instead a primary
  button opens Android's own *Memory used by apps* screen
  (`android.settings.APP_MEMORY_USAGE`, averaged over 3 h–1 day), falling back to
  Developer options when the OEM has removed that screen.
- **Apps ranked by measured storage** from `StorageStatsManager`: app, user data and
  cache bytes for every user-facing or recently used app, with a relative bar. This is
  storage, and it is labelled as storage — not RAM.
- **Activity** (last 24 h) from `UsageStatsManager`: last opened, time on screen,
  and whether a background service ran. Sort by *Largest first* or *Recently used*.
  Needs *Usage access*, which the app requests with a deep link into Settings.
- **Per-app sheet** with the storage breakdown, activity, and a one-tap jump to
  Android's App info page, where Force stop and Clear cache live. System apps are
  flagged before you stop them.
- **Free up memory** button on Android 8–13 (`killBackgroundProcesses`), with the
  measured amount freed. On Android 14+ that API only affects the calling app, so
  the button is replaced by an explanation.

## What it deliberately does not do

Since Android 8, third-party apps cannot see other apps' memory or kill their
processes without root. MemWatch does not fake either. Anything claiming otherwise
on a stock phone is either using an accessibility hack or just showing cached numbers.
Three different numbers are kept visibly apart: **RAM** (device-wide, and per app only
inside Android Settings), **storage** (per app, measured), and **activity** (per app,
from usage stats).

## Build

Requires JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```sh
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Every pull request also builds the debug APK in CI and uploads it as the
`memwatch-debug-apk` artifact.

## Stack

Kotlin 2.0, Jetpack Compose + Material 3, single-activity `StateFlow` view model,
min SDK 26, target SDK 35. No network access, no analytics, no accounts.
