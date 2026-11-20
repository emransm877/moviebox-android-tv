# Android TV release process

1. Keep release.json, Gradle versionName/versionCode, native/shared updaters,
   user-agent version, README and release notes consistent. Run
   `python tools/check_release.py`.
2. Build and run unit/lint checks with the pinned wrapper and dependencies.
3. Supply `MOVIEBOX_TV_KEYSTORE`, `MOVIEBOX_TV_STORE_PASSWORD`,
   `MOVIEBOX_TV_KEY_ALIAS` and `MOVIEBOX_TV_KEY_PASSWORD` through a secure build
   environment. Never put secrets in shell arguments, Git or release assets.
4. Run `python tools/build_release.py`. Verify the APK using Android SDK
   `apksigner verify --verbose --print-certs` and `zipalign -c -P 16 4`.
5. Test the exact signed, non-debuggable APK. Generate the test video with
   `python tools/create_fixture.py`, build the same-signed test APK using
   `:app:assembleReleaseAndroidTest -PandroidTvReleaseTests=true`, and run
   `SharedTvReleaseScenarios`. Run cold-process restoration separately after
   stopping the app. Do not publish the instrumentation APK or video fixture.
6. Record source commit, certificate fingerprint, APK hash, target and actual
   scenario results. Debug runtime checks alone do not validate the release.
7. Publish the reviewed `MovieBox-Android-TV-VERSION.apk`, its `.sha256` and notes
   under `vVERSION` after maintainer approval. Do not silently replace assets.
8. Verify public bytes and checksums, then update hub links, the Android TV guide
   and analytics repository list together.

The release keystore stays outside Git and needs secure backup. Future in-place
updates require the same signing identity. CI produces review outputs and never
automatically publishes a new release.
