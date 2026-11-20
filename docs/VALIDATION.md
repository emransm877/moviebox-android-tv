# Android TV validation

Development began on 21 November 2026 (Asia/Dhaka). This document describes
bounded checks, not a universal device certification.

## Published 0.1.0 signed artifact

The non-debuggable release APK passed same-certificate instrumentation on the
Android TV API 36 x86_64 target at 1920×1080. Native Media3 decoded the generated
65-second 1080p H.264/AAC video; the shared frontend rendered generated subtitles.
Checks included OK pause/resume, forward/backward and repeated seeking, audio
selection, drawer activation only from hidden controls, sparse episode 4→7
autoplay, final return, failed-source Back exit, and navigation to Movies/TV/My
list/home during playback. A separate instrumentation process verified saved
library, theme and progress after force-stopping the application.

Both signed-release instrumentation invocations passed. v1/v2 APK signature
verification passed; debug mode is disabled. The exact APK hash and scenario
identifiers are in [signed-release-runtime.json](signed-release-runtime.json).
The release also passed unit checks and release lint-vital checks. The previous
debug suite additionally checked all five themes, native keyboard and lifecycle
restoration. That additional debug evidence is not a physical-device guarantee.

No physical Android TV device was available. Minimum Android/WebView versions,
all TV brands and complete live-service episodes are not certified by these tests.

## Current shared-interface runtime checks

The launcher is now `SharedTvActivity`: the existing TV frontend plus native
Android Media3, remote events, keyboard and app-private atomic storage. The
checks in `verification/shared-runtime.json` describe this launcher. Screenshots
come from Android screencap, because WebView CDP screenshots can duplicate GPU
tiles and do not reliably capture the native video surface.

Runtime testing identified and fixed Android density causing a cropped viewport,
missing embedded-audio selection forwarding, stale WebView library data hiding
newer Android saves after restart, and a reused player's prepare callback being
cleared. Platform transport now identifies Android correctly and uses the native
player with bounded startup recovery. No shared Tizen stylesheet was changed.

Use the latest complete runtime JSON for the final passing scenario list. The
synthetic 65-second 1080p H.264/AAC fixture and generated SRT exercise actual
native decoding; they are not a full-length service-video certification.

## Earlier native-widget prototype observations

- SDK: Android TV API 36 x86_64, 1920×1080, WHPX virtualization.
- JDK 17, Gradle 8.9, Android Gradle Plugin 8.7.3, Media3 1.8.1.
- First native launch loaded 198 actual catalogue subjects and title artwork.
- Service details loaded actual season and sparse-episode metadata.
- Four unit policy cases passed: sparse episodes, bounded seeks, stable release
  version ordering and rejection of locked/wrong-episode resources.
- Initial three native acceptance scenarios passed with generated 1080p media:
  decoded playback, immediate OK pause/resume, repeated seeking, disk resume,
  terminal movie return, failed-source remote exit, all five theme tokens and
  My list persistence after Activity recreation.

- The expanded four-scenario native suite passed, including a selected generated
  SRT track and the Up shortcut opening the episode drawer only with hidden
  controls. Sparse episode order was retained in the drawer scenario.
- A real service selection, Love in Disguise [Hindi], S1 E1, rendered video in the
  native player with observed progress around 42 seconds and a reported runtime
  of 1:04:49. This is an observed playback check, not a complete-episode run.
- Lint completed with no errors. Warnings include pinned dependency versions and
  target-SDK compatibility; those remain review items before production release.

The latest saved test output determines the current result. No physical Android
TV compatibility or completed full-length live playback certification is claimed.

## Before release

- Re-run tests against the final signed APK and preserve its SHA-256.
- Inspect all five themes for clipping, focus order and artwork identity.
- Exercise real search, Movies/TV filters, subtitle discovery, audio selections,
  complete series transitions, cold restart and offline/rate-limited updates.
- Verify remote navigation during slow and failed network/decoder operations.
- Validate minimum supported Android and available physical Android TV hardware.
- Complete the feature-parity items listed in README before claiming parity.

No “all devices,” zero-bug or guaranteed-smooth claim follows from these checks.
