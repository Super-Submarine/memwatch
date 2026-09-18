# MemWatch

A small Android app that answers "why is 1 GB of my RAM in use when nothing is open?"
and gets you to Android's own **Force stop** button in one tap.

## What it shows

- **Real RAM numbers** from `ActivityManager.getMemoryInfo()` and `/proc/meminfo`:
  in use, available, cached and swap (zRAM), plus a Healthy / Tight / Critical badge
  based on the kernel's own low-memory threshold.
- **Recently active apps** (last 24 h) from `UsageStatsManager`: when each app was
  last opened, time on screen, and whether it ran a background service. Needs
  *Usage access*, which the app requests with a deep link into Settings.
- **Per-app sheet** with a one-tap jump to Android's App info page, where Force stop
  lives. System apps are flagged before you stop them.
- **Free up memory** button on Android 8–13 (`killBackgroundProcesses`), with the
  measured amount freed. On Android 14+ that API only affects the calling app, so
  the button is replaced by an explanation and a link to Developer options →
  Running services, which is the only place per-app RAM is still visible.

## What it deliberately does not do

Since Android 8, third-party apps cannot see other apps' memory or kill their
processes without root. MemWatch does not fake either. Anything claiming otherwise
on a stock phone is either using an accessibility hack or just showing cached numbers.

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
