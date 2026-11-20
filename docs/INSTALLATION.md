# Install MovieBox on Android TV / Google TV

Get `MovieBox-Android-TV-0.1.0.apk` from the
[release](https://github.com/emransm877/moviebox-android-tv/releases/tag/v0.1.0).
The installed name is **MovieBox**. This APK is separate from the phone patch
and cannot install on Samsung Tizen.

## TV file manager

1. Transfer the APK using your preferred TV transfer method or supported USB storage.
2. Allow **Install unknown apps** for your chosen file manager in Android settings.
3. Open the APK, install, then launch MovieBox from the TV launcher.
4. Disable the file manager's installation permission afterwards if preferred.

Keep Android System WebView updated. If installation fails, check the OS version,
free space and that the device actually runs Android TV. WGT and EXE files are
for different operating systems.

## From a PC

Enable developer options and debugging on your TV and approve your own PC.
With Android platform-tools, choose the exact device serial:

```text
adb devices
adb -s YOUR_TV_SERIAL install -r MovieBox-Android-TV-0.1.0.apk
```

Use your TV's actual debugging address, never the maintainer's private address.
You can disable debugging afterwards. Root access is not required.

## Verify the file

```powershell
Get-FileHash -Algorithm SHA256 .\MovieBox-Android-TV-0.1.0.apk
```

Compare with the release's `.sha256` file. Android SDK
`apksigner verify --print-certs` verifies the APK signature; the signing fingerprint
is in [release.json](../release.json). Keep the APK and checksum together.

## Controls

Arrows move focus, OK selects, Back returns. OK pauses/resumes on the play control
or unobstructed player. Left/Right seek from hidden controls or the seek bar.
Up opens seasons/episodes only when controls are hidden; the episode icon opens
the drawer explicitly. Activating search opens Android's installed TV keyboard.

## Updates and data

New versions keep the same application ID and signing certificate. Install a newer
signed APK with `-r` or your TV file manager to preserve private library data.
Optional notices never force installation or promise instant delivery while closed.

An old local debug prototype has a different certificate. Android rejects an
in-place update from that prototype to the public release. Uninstalling removes
its private data; keep anything you need first. Normal signed public updates do
not require uninstalling the previous public version.

Uninstall through Android Settings. Clearing app data or uninstalling removes the
saved library and subtitle cache.
