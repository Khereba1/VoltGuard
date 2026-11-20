# VoltGuard — AI Handoff

## Identity
- **App Name:** VoltGuard
- **GitHub Repo:** `Khereba1/VoltGuard`
- **applicationId:** `com.khereba1.voltguard`
- **Source namespace:** `com.khereba1.app`
- **Version:** `1.0.0` (`versionCode = 1`)
- **Theme & Language:** Minimal Dark Premium Material 3 (system-following Dark/Light + Arabic/English), embedded `IBM Plex Sans Arabic` + `IBM Plex Mono` (downloaded automatically in `.github/workflows/build.yml`).

## Architecture
- `App.kt` — `App` + `Prefs` (bypass & auto-boot flags).
- `BootReceiver.kt` — Re-applies hardware bypass on `BOOT_COMPLETED` via `goAsync()`.
- `DeviceMonitor.kt` — `RootShell` (auto-detects Samsung `store_mode` / `batt_slate_mode`, generic `input_suspend` / `charging_enabled`, `top` + `dumpsys power` WakeLocks, Reboot/Recovery) + `DeviceMonitor` (Battery telemetry, per-core CPU frequencies, RAM/Storage, Network/IP).
- `MainActivity.kt` — Swipeable `HorizontalPager` (Battery / System / Network), live current sparkline, collapsible Top Processes & WakeLocks (root-only), 2-column per-core CPU grid, Copy IP.

## Update Workflow
For future updates, deliver a single `fixN.zip` containing only changed files (root-relative paths), increment `versionCode` and `versionName` in `app/build.gradle.kts`, and keep `values/strings.xml` and `values-ar/strings.xml` synchronized.
