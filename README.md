# CheckIt

CheckIt is a Kotlin Multiplatform calendar and task management app for Android and iOS, built with shared Compose UI in `shared`.

The app is designed around blending the calendar aspect with the completion aspect, focusing on how you spend your days, keeping a record of your time and wins.

## Features

### My Day
- Plan a single day with a daily plan of timed items
- Item sources: existing tasks, My Day tasks, notes, and reminders
- Planned / Done status with optional start and end times (time tracking)
- Views: Timeline, Agenda, and Board
- Suggest open tasks and auto-add tasks due today onto My Day
- Evening day review: summary, leftover actions (done / carry to tomorrow / leave), optional win note
- Morning leftovers: carry unfinished yesterday items into today (manual or auto-carry setting)
- Plan assist banner when My Day is empty after plan reminder time
- Markdown-style summary of completed work for the day

### Tasks
- Tasks with description, subtasks, priority, do date, and optional time range
- Per-task reminders
- Recurrence support via RRULE (data model and editor; next-instance generation on complete is still limited)
- Notes alongside tasks in the same workspace
- Tags with color, plus inline `#tag` parsing and suggestions
- Lists (objectives) with icon and color
- Soft trash and restore for tasks and notes
- Search, sort (custom / priority / title / date), and list density (Brief / Standard / Detail)
- Workspace views: List, Agenda, Timeline, and Goal
- Built-in filters: All, Today, Upcoming, Overdue, No date, Completed

### Goals and OKRs
- Goals → Objectives (lists) → Key Results hierarchy
- Key result units: percentage, number, currency, hours, days, points, binary
- Progress from current vs target value
- Sync key-result progress from completed daily-plan work where applicable

### Calendar
- Month calendar with activity markers
- Past days reflect daily-plan activity; future days show scheduled tasks and notes
- Filter calendar markers by tags
- Daily plan summary for a selected date

### Reports
- Digest report: totals, done vs planned, trends, and highlights
- Time report: minutes worked across periods
- Tags report: time spent by tag
- Period selection (daily and longer ranges)

### Notifications and reminders
- Plan-the-day, evening review, check-in, and next-scheduled-item reminders
- Configurable enablement and times in Settings
- Quiet hours (approx. 22:00–06:00) for notification policy
- Android workers for app, task, and schedule reminders

### Settings and platform
- Language: English, Vietnamese, Chinese
- Theme: System, Light, Dark
- Color schemes: Sunset, Sky Blue, System default
- Local persistence (Room) and preferences (DataStore)
- Android home-screen widgets (daily plan agenda, check-in) with deep links into the app
- Shared Compose UI for Android and iOS via Kotlin Multiplatform

## Project Structure

- `androidApp`: Android app shell.
- `shared`: Shared Kotlin Multiplatform code, Compose UI, domain logic, and persistence.
- `iosApp`: iOS app shell.

## Development

### Prerequisites

- JDK 17+
- Android SDK (compileSdk 37, minSdk 28, targetSdk 37)
- Xcode 15+ (for iOS)

### Useful Verification Commands

```shell
# Android debug APK
./gradlew :androidApp:assembleDebug

# Shared tests
./gradlew :shared:allTests

# iOS simulator framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## GitHub Release APK

Releases are tag-driven. Pushing any Git tag starts the GitHub Actions workflow, builds a signed Android APK, and attaches it to a GitHub Release.

The workflow builds APK only; it does not build an Android App Bundle (`.aab`).

```shell
git tag v1.0.1
git push origin v1.0.1
```

### Required GitHub Secrets

Before using the release workflow, add these repository secrets in GitHub under Settings > Secrets and variables > Actions:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: release key password.
- `GOOGLE_SERVICES_JSON`: optional Firebase `google-services.json` content. If omitted, CI uses a dummy config so the direct-download APK can still be built.

Update the `GOOGLE_SERVICES_JSON` secret from the local Android config with:

```shell
gh secret set GOOGLE_SERVICES_JSON < androidApp/google-services.json
```

### Release Keystore

To create a new release keystore:

```shell
keytool -genkeypair -v \
  -keystore checkit-release.jks \
  -alias checkit \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

On macOS, copy its base64 value for the `ANDROID_KEYSTORE_BASE64` GitHub secret:

```shell
base64 -i checkit-release.jks | pbcopy
```

## Local Release Build

The generated release keystore for this workspace is stored at:

```text
.secrets/checkit-release.jks
```

Signing values are stored in:

```text
.secrets/checkit-signing.env
```

The `.secrets` directory is ignored by Git and must not be committed.

To build a signed release APK locally with that keystore:

```shell
set -a
source .secrets/release-signing.env
set +a

ANDROID_KEYSTORE_PATH="$PWD/.secrets/checkit-release.jks" \
VERSION_NAME=1.0.1 \
VERSION_CODE=101 \
./gradlew :androidApp:assembleRelease
```

The APK is written to:

```text
androidApp/build/outputs/apk/release/
```
