# Changelog

## 0.2.0 - 2026-11-23

- Adds 20 visual themes and bounded 3D poster carousels while preserving the five
  existing theme identities and saved choices.
- Adds compact Continue watching cards, sidebar navigation and visual Settings.
- Restores carousel rotation after manual browsing becomes idle; avoids repeated
  cover/language editions in the featured queue without removing catalogue entries.
- Adds a narrow, semi-transparent in-player season/episode dock fitted between
  the player header and controls, with scrolling confined to the episode list.
- Preserves Android-native Media3 playback, remote input, keyboard and private
  library storage. New-theme background restoration follows the canonical palette.
- Unsupported older WebView providers show upgrade guidance instead of a blank UI.
- Retains application ID and release signing identity; version code increases to 2.

The interface/native runtime checks were performed
on an API36 Android TV emulator in the preceding same-source development build;
physical Android TV hardware remains unverified. Current release build and
signature checks are recorded separately; no universal compatibility claim is made.

## 0.1.0 — 2026-11-21

First signed independent Android TV / Google TV preview. Installed name: MovieBox.

- Shared TV interface/themes with native Media3, remote events, keyboard,
  service networking and atomic library persistence.
- Discovery, carousel, artwork, search, details and available streams.
- Subtitle/audio controls, bounded seeking, resume, My list, sparse episode
  continuation and a right-side player episode drawer.
- Fixed Android density cropping, embedded-audio forwarding and stale WebView
  state masking newer Android saves after a cold restart.
- Checked the exact signed release using generated 1080p native media, controls,
  repeated seeks, subtitles/audio, series progression, failure exit, navigation
  during playback and a separate cold-process restore.

Hardware/minimum-OS coverage remains incomplete. Background downloads, local file
integration and account flows are not complete desktop/phone parity. Available
languages and qualities depend on the source.
