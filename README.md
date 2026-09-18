# MemWatch

A small Android app that answers "why is 1 GB of my RAM in use when nothing is open?",
shows **which app is using how much RAM right now**, and force stops it in one tap.
It only ever talks about RAM — never storage, never "recently used" as a stand-in.

## What it shows

- **Real RAM numbers** from `ActivityManager.getMemoryInfo()` and `/proc/meminfo`:
  a stacked bar of in use / reclaimable cache / free, available, swap (zRAM) and the
  kernel's low-memory threshold, plus a Healthy / Getting tight / Low memory badge.
  Works with no setup at all.
- **RAM by app** — every running process, grouped by the app that owns it, ranked by
  live **PSS** with a relative bar. PSS (proportional set size) is the number Android
  itself uses: an app's private memory plus its fair share of memory shared with other
  processes, so the column adds up to what the device is really using. Each row also
  says how Android ranks the app for reclaim (*In use*, *Visible*, *Background
  service*, *Always running*, *Cached — reclaimable*, …) and how many processes it has.
  Processes that don't belong to an installed app are kept honest in two groups:
  *Native & kernel processes* and *Android System*.
- **Per-app sheet**: RAM right now, each process with its own PSS, what the reclaim
  tier means, **Force stop** (`am force-stop`, the same thing Settings does — system
  apps get a confirmation first) and a link to Android's App info page.
- **Clear N cached apps · X MB** (`am kill-all`): drops exactly the processes Android
  already considers reclaimable, and reports how much was actually freed afterwards.
- **Android's own RAM screen** (`android.settings.APP_MEMORY_USAGE`, a 3 h–1 day
  average) stays one tap away as a second opinion.

## Why Shizuku

Since Android 8 (and stricter on 10+) an ordinary app cannot read other apps' RAM or
stop their processes. Apps claiming otherwise on a stock phone show cached or invented
numbers. The only honest, no-root way is [Shizuku](https://shizuku.rikka.app/): it runs
a tiny helper with the same `shell` privilege as a USB-debugging PC, and lends that to
apps you approve. MemWatch uses it to run `dumpsys meminfo -c` (the per-process PSS
list) and `am force-stop` / `am kill-all`.

One-time setup, guided in-app with a three-step checklist that tracks your progress:

1. Install Shizuku (Play Store or GitHub).
2. Start it once: in Shizuku, *Start via Wireless debugging* → pair (Android 11+).
   Shizuku must be started again after a reboot; MemWatch tells you when that's needed.
3. Allow MemWatch when Shizuku asks. Denying is remembered; the card explains how to
   re-allow from Shizuku's *Authorized applications* list.

Rooted phones can start Shizuku with root instead; Android 10 and below need one `adb`
command from a PC.

### Without Shizuku

The phone-wide RAM card, the explanation of why cache counts as "in use", and the link
to Android's own RAM screen all work with nothing installed. The *RAM by app* section
shows the setup card instead of pretending to know per-app numbers.

### When something goes wrong

Every failure is shown in place of the list with a retry: Shizuku stopped (after a
reboot), permission denied, the helper process disconnected, a command timing out
(20 s), a command failing, or `dumpsys` returning nothing parsable. If a list was
already on screen, it stays, marked "last refresh failed: …" so stale numbers are never
mistaken for live ones.

## What it deliberately does not do

- It does not show storage, install size or "last used" anywhere — those are not RAM.
- It does not claim to *kill* processes for good. Force stop frees the memory now;
  Android will restart system apps and anything with a scheduled job, and the app says
  so before you tap.
- It does not read per-app RAM without Shizuku. There is no permission an ordinary
  app can request for that; a setup card is the honest state.
- MemWatch's own two processes (the UI and the Shizuku helper) appear in the list like
  everything else.

## Build

Requires JDK 17 and the Android SDK (platform 35, build-tools 35.0.0).

```sh
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Every pull request also builds the debug APK in CI and uploads it as the
`memwatch-debug-apk` artifact.

To try the Shizuku path on an emulator: install Shizuku, then start its server over
`adb` using the command the Shizuku app shows under *Start via ADB* (on a fresh
emulator the starter binary is `lib/<abi>/libshizuku.so` inside Shizuku's APK
directory, run with `--apk=<path to base.apk>`).

## Stack

Kotlin 2.0, Jetpack Compose + Material 3, single-activity `StateFlow` view model,
Shizuku API 13 (AIDL user service running `sh -c` as `shell`), min SDK 26, target
SDK 35. No network access, no analytics, no accounts.
