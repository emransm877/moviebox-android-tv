# MovieBox Android TV 0.2.0

23 November 2026 (Asia/Dhaka). Independent Android TV / Google TV preview.
Installed app name: **MovieBox**.

## What changes

- Twenty visual themes with bounded 3D poster layouts and clickable title artwork.
- A consistent sidebar, compact Continue watching cards and visual Settings.
- Automatic carousel movement resumes after manual browsing becomes idle.
  Duplicate featured cover/language editions are removed from the hero queue;
  individual catalogue editions remain available.
- A narrow semi-transparent season/episode dock fits between the player header
  and controls, with a scrollable episode list and fixed header/footer.
- Android-native Media3 playback, remote arrows/OK/Back, native search keyboard,
  subtitles and app-private library persistence remain in place.
- New-theme background restoration uses the shared palette. Unsupported old
  WebView providers receive upgrade guidance instead of a blank screen.

## Installing and updating

Release filename: `MovieBox-Android-TV-0.2.0.apk`. It uses versionCode 2, the
existing `org.emransm.moviebox.tv` package and the same release certificate:

```text
5c2759fa8da534debbc37200380670660c1780c7f701000890537e48e7a1614b
```

The final SHA256 is supplied beside the staged APK. This release is intended as
an in-place update; uninstalling is unnecessary and would remove local app data.

## Evidence and limits

The preceding same-interface development build was exercised on Android TV API36
in an emulator, including all20 themes, native Media3 playback of owned1080p media,
subtitles, seeking, sparse episodes, the final dock geometry and saved-state
restoration. Those are historical bounded runtime observations, not a fresh
runtime certification of this version-only candidate. Version 0.2.0 passed seven unit tests, release lint/build, signature, package/version
and all 63 shared plus 4 Android platform packaged-asset checks. Lint retained 46
warnings and no errors.

No physical Android TV was available. Codec, remote and WebView differences
remain device-dependent. Android6/API23 is the manifest minimum, with a compatible
current WebView required; old WebView44 shows upgrade guidance and does not run
the shared UI. Service-provided languages, qualities and subtitles remain subject
to upstream availability. Complete phone/desktop feature parity is not claimed.

Updates remain optional. A source commit alone does not prompt an app update; a
new published release is checked by the app. See [validation](VALIDATION.md) and
[release process](RELEASE_PROCESS.md).
