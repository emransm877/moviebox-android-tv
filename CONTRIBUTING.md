# Contributing

Keep changes small and preserve controls, storage and release identity. Use JDK 17
and the Gradle wrapper. Run release-contract checks, unit/lint checks and relevant
runtime scenarios. Mark checks not performed explicitly.

Report model, Android/WebView versions, app version, expected behavior and steps.
Separate a UI freeze from an unavailable stream. Remove private data from captures.

Refresh the shared frontend with `tools/sync_tv_ui.py`, preserving CSS hashes and
Android adapter boundaries. Do not add upstream APKs, film files, credentials,
signing keys or private logs. Releases require review of the exact signed files.
