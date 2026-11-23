# Local development

Clone this repository and work from its root. Development checks run locally by default; GitHub remains the source host and retains necessary production automation.

## Environment

Run `python tools/local_doctor.py` (`python3` on systems where that is the Python 3 command). This read-only preflight detects the host and basic tools; it does not install dependencies or certify a build. Use the project's supported runtime versions below and existing lockfiles. Do not upgrade dependencies just to run checks.

For Python, create an isolated environment with `python -m venv .venv`. Use `.venv/Scripts/python.exe` on Windows and `.venv/bin/python` on Linux/macOS for installation and checks (activation is optional). Replace `python` below with that interpreter. Existing pinned requirements remain authoritative. Where requirements contain ranges, exact historical dependency resolution is not guaranteed; record a tested lock separately before upgrading.

For Node projects with package-lock.json use `npm ci`; without a lockfile use `npm install` and review the generated lock before committing. Use the declared packageManager if present. Native SDKs and first dependency downloads need internet access. Local-first does not make external APIs or production services offline.

## Repository commands

Use Python 3.12, Node 22, JDK 17, SDK platform android-36 and build-tools 34.0.0. Run `python tools/check_release.py` and `node --test tests/preflight.test.cjs`.

Windows: `gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Linux/macOS: `bash gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Validate navigation and playback on a compatible TV/emulator separately. Existing MOVIEBOX_TV_* environment variables configure release signing; do not reuse debug signatures for updates.

## Existing hosted command reference

These are the pre-migration commands for reproducing checks locally. Use the correct host shell (PowerShell paths on Windows, Bash on Unix); install requirements from the workflow. Publishing commands are intentionally excluded.

### checks.yml / build

```text
python tools/check_release.py
node --test tests/preflight.test.cjs
sdkmanager 'platforms;android-36' 'build-tools;34.0.0'
bash gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/MovieBox-Android-TV-0.2.0-debug.apk
cd artifacts
sha256sum MovieBox-Android-TV-0.2.0-debug.apk > SHA256SUMS.txt

```


## Automation and releases

Development workflows are manual fallbacks; select Run workflow in GitHub only when local execution is impractical. Retained production workflows keep their existing triggers and approval gates. Existing release downloads are unaffected by changing development triggers. Manual workflow artifacts expire after the retention shown in each workflow and are not durable release downloads.

Before pushing a workflow change, inspect current branch protection/rulesets and preserve required check names or coordinate their replacement. A manual workflow does not automatically satisfy required PR checks. Never weaken protection merely to make a push pass.

Rollback: inspect the migration commit with `git show <migration-commit>`, then `git revert <migration-commit>` and push under normal repository policy. Reverting automatic triggers may start cloud jobs; review the resulting diff first. Avoid reverting later production state or historical statistics. Account billing and Actions permissions were not changed.

For a new project, choose its existing/native tooling, record supported runtime versions, commit an appropriate dependency lock and environment example without secrets, define local validation commands and platform limits, and classify any proposed cloud automation before adding it. Do not copy an unrelated stack's build script.
