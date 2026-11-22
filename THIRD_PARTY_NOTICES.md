# Third-party notices

The original client code is MIT licensed. This does not transfer rights to
upstream applications, service metadata, catalogue artwork, logos or trademarks.

- AndroidX RecyclerView, Media3 and AndroidX test libraries: Apache License 2.0.
  Sources: https://android.googlesource.com/platform/frameworks/support/
  and https://github.com/androidx/media .
- AndroidX WebKit: Apache License 2.0; the Android System WebView provider is
  supplied and updated by the device, not bundled as a browser runtime.
- Player vectors: MIT-licensed Stremio Icons by Smart Code Ltd. and contributors.
  Full notice and provenance are included in
  `app/src/main/assets/tv-ui/assets/player-icons/LICENSE.txt` and `SOURCE.json`.
- Guava and related transitive library components retain their upstream notices.
  Source: https://github.com/google/guava .
- Gradle wrapper files retain Gradle's upstream license notices.
  Source: https://github.com/gradle/gradle .
- The theme palettes and community brand assets are shared with the existing
  MovieBox Desktop/Tizen projects. Catalogue and promotional artwork are requested
  from their existing upstream URLs; no ownership of that artwork is claimed.
- The native service adapter uses the same recovered public wire-protocol
  compatibility fields as the desktop and Tizen adapters. It does not contain an
  upstream signing certificate, user account token, phone session or hardware
  attestation. Runtime guest sessions are obtained normally and are not committed.

The app does not bypass memberships, DRM or service access requirements. Locked
resources and placeholder media are excluded rather than labelled as playable.
