# CheckIt

CheckIt is a Kotlin Multiplatform calendar and planning app for **Android, iOS, and macOS**, built with a shared Compose Multiplatform UI in `shared`.

The app is designed around blending the *calendar* aspect with the *completion* aspect: focusing on how you spend your days, keeping a record of your time, and keeping a record of your wins.

---

## Features

### My Day

The daily hub. A swipeable, two-segment page (plan / quick notes) with a segmented sub-view switcher.

- **Three views**: Agenda, Timeline, Routines
- **Timeline**: 24-hour column with drag-to-resize start/end times and drag-on-empty-space to create a new item at that range
- **Agenda**: date-grouped list of tasks, notes, plan items, and journal entries
- Plan items can come from **existing tasks, My Day tasks, notes, or reminders**
- **Planned / Done** status with optional start and end times (time tracking)
- **Period goal banners** for day / week / month, with three states: missing goal, active goal, and review pending (day review is prompted after 19:00)
- **Smart Schedule** — proposes times for unscheduled items by learning your usual time-of-day per tag from the last 3 days of completed work, then running a conflict-aware greedy assignment. Runs fully on-device (see [Notes on "AI"](#notes-on-ai))
- **Add-to-My-Day suggestions**: suggested tasks, yesterday's leftovers with per-item *Carry over* or *Carry all*, inline quick-add
- **Day Close** evening review: done / planned / minutes summary, per-tag minutes, today's goal for context, a rotating win-note prompt, a 5-star rating, and tomorrow's top 3 priorities
- Per-leftover **FilterChips**: Done / Carry over / Drop
- **Plan assist banner** when My Day is empty after the plan reminder time
- **Floating quick-add bar** that appears when nothing is scheduled within ±15 minutes of now
- **SpeedSprint FAB** (tap repeats the last sprint; long-press expands into Quick Sprint and per-recent-tag sprints)
- **Celebration overlay** ("Great work today!") when the day is complete
- Markdown-style summary of completed work for the day
- Once-a-day auto pass on resume: auto-add tasks due today, rebuild reflect rollups

### Focus Sprints (timer / Pomodoro)

- Start a sprint from a plan item, a bare task, or ad-hoc free text
- **Quick Sprint**, **Deep Focus (Pomodoro)**, and **Short Break** modes with distinct color and label
- Quick Sprint sheet suggests Today's Plan (top 5), Yesterday's Leftovers (top 3), tasks (top 5), and *Continue Last*
- `SprintBar` countdown overlay: `MM:SS` timer, linear progress, pause/resume/stop
- Mode-aware completion dialog (e.g. break → *Next Pomodoro*; quick → *Start Pomodoro*), plus **Save as a win**
- Saved wins surface in the Calendar **Hall of Fame**

### Routines

Reusable daily checklist templates that reset each day.

- Title, description, reminder time, weekday repeat picker, color, and an ordered list of **steps**
- Per-day progress: "X of Y steps (Z%)" with animated progress bars, overall and per routine
- Long-press drag to reorder routines and to reorder steps
- Steps tap-to-check, struck through when done and dimmed when not scheduled for today
- Inline step editor (Enter adds the next row and focuses it)
- Dedicated reminders, heatmap, and streak tracking (a day counts toward the streak at **≥ 80 %** completion, and off-schedule days are skipped rather than breaking the streak)

### Quick Notes (capture inbox)

A capture-first inbox for raw thoughts, reachable as the second My Day segment.

- Quick capture bar with camera button
- **NEXT list** with tap-to-select, long-press drag reorder, and haptics
- **Swipe gestures with detents**:
  - past 1 detent → *Remind Me Later* (15m / 30m / 1h); the note moves to the top when reminded
  - past 2 detents → copy the text into My Day plan
- Row actions: **star** (high priority), **timer** (start countdown), **delete**
- **Countdown** presets (5m / 10m / 15m / 30m / 1h) show a live `MM:SS` chip on the row and a live timer in the notification
- **Camera capture** → *Add photo* dialog with preview and title; thumbnails render inline, tap for full screen
- **"TO BE DELETED"** section: swipe either way — one direction deletes permanently, the other restores. Deleted notes disappear after 24 hours
- Explicit sync status banner (syncing / offline / last synced) with Retry

### Tasks, Notes, Lists and Tags

- Tasks with description, subtask checklist, priority, do date, optional time range, and per-task metrics
- Notes alongside tasks in the same workspace (title + markdown body, date, start time, list, tags)
- **Task type**: `Task` or `Habit` — habits are repeat-rule driven rather than do-date driven
- **Lists** with icon and color, plus draggable color-coded **list sections**
- **Tags** with color, usage counts, drag reorder, inline `#tag` parsing with fuzzy matching, and a suggestions picker
- **Repeat rules**: `None`, `Daily`, `Weekly` (with a weekday set), `Monthly` (with day-of-month clamping) — deliberately *not* iCal/RRULE (see [Notes on "AI"](#notes-on-ai))
- **Soft trash** and restore for tasks and notes
- Search, sort (Custom / Priority / Title / Date), and list density (Brief / Standard / Detail); drag reorder when sort is Custom
- **Four workspace views**: List, Agenda, Timeline, Habits
- **Built-in filters**: All, Today, Upcoming, Overdue, No date, Someday, Completed, Trashed
- **Metrics** per task: name, value, target, and unit (Percentage, Points, Items, Hours, Days, Rating, VND, Lạn, Km, Countdown, DueDate, Custom) with progress bars
- Task editor overflow: Pin/Unpin, Complete, Reopen, Delete, and **Schedule** (add to My Day)
- Sidebar drawer with per-list counts; FAB menu for **Task / Habit / Note**

### Nested Lists (outline documents)

A full outliner for long-form, hierarchical documents.

- Multiple documents in a drawer: rename, delete, create; auto-reopens the last-edited document
- **Toolbar**: zoom in/out, expand/collapse all, indent/outdent, move up/down, add child, add sibling, delete, add root, manage details, selection mode, sort children, move to…, add to My Day, copy to task
- **Long-press drag and drop** with depth-colored guide lines, drop indicator, edge auto-scroll, and subtree dragging
- **Breadcrumb** zoom path and display modes (All / Working only)
- **Filter bar**: date-range focus (prev/next/current), text query, hide checked, tag selection, reset
- **Formatting bar** on the selected item: text style (body/headings/checkbox), text and background color tokens, priority, date range, tags, check off
- **Selection mode**: batch set-checked, batch delete
- **Item details**: metric settings (minutes, policy, show progress, progress slider) and manual metrics
- **Documents sync** to Firestore (manual trigger, per-document sync button with spinner)

### Calendar

- **Month and Week** display modes with prev/next navigation and tap-to-jump-to-today
- **Year view** with per-month mini calendars and a rating-level distribution legend
- Date cells show **activity markers (counts)** and **rating stars**; past days use precomputed rollups, today/future use live plans
- **Double-tap a date** to add a My Day plan item (allowed within the last 3 days and into the future)
- **Tag filter** in the top bar filters all content on the selected date
- Selected-date header: date block, weekday, Today/Yesterday/year, count badges for tasks / notes / journal (journal badge opens the journal list for that day), and star rating
- Expandable popup with the day review text (link to Reflect) and day/week/month goal rows
- **Hall of Fame** — a trophy handle expands into a gallery of the month's wins with date, star rating, and win note; tap to jump to that date
- **Journal history** accessible from the top bar

### Reflect (reports, reviews, goals)

- **Five periods**: Daily, Week, Month, Annual, Habit, with prev/next, jump-to-current, and zoom between levels
- **Period goals** — a narrative document per period (`Day`, `Week`, `Month`, `Quarter`, `Year`): review text, goal text, 0–5 satisfaction rating, and metrics
- **Period goal editor** with per-period reflection prompts, a 5-star `RatingBar`, parent-goal context in Goal-only mode, and **markdown templates**:
  - Review — *Wins & Friction*, *Weekly Review*, *Highlights*
  - Goal — *Top 3 Focus*, *If-Then Plan*, *Milestones*
- **Goal & reflection card** plus a paged **previous-period history sheet** (Paging 3)
- **Activity & Tags**: total time, a "Your Rhythm" bar chart (tap a bar to zoom into that day), and tag-hours meters
- **Past Days Chronicle**: one row per sub-period with date, tracked time, rating, goal quote, review text, and metric chips
- **Habit period heatmaps**: per-habit streak and totals, and routine heatmaps, each covering the last 2 months
- **Guidelines** — an in-app markdown reader over a user-selected folder of `.md` files

> Note: CheckIt uses **Period Goals** (narrative day/week/month reviews with ratings and metrics). There is no Goal → Objective → Key Result OKR model; "Key Objectives" exists only as a markdown template and "OKR" as a help-tooltip example.

### Journal

- **Write / Details** dual-mode entry editor
- **Write**: prompt picker, large markdown field with a cursor-aware markdown toolbar, sticky footer with word count, estimated read time, and a rotating follow-up nudge
- **Details**: label field with suggested chips, **mood row** (8 categories with emoji, expandable, plus a full emoji picker), *"I feel X because…"* feeling chips, and tags
- **Journal prompts** filtered by time of day (morning / afternoon / evening) across State of Mind, Reflection, and Capture categories, each with a guiding question and a markdown template
- **Labels**: gratitude, growth log, deep thoughts, idea, random, lazing, biking, coding, reading, learning, event, at home
- **Moods**: Happy, Energetic, Calm, Loved, Focused, Tired, Worried, Sad
- **History browser** with mood filter chips, search, tag filter, agenda interleaved with period goal cards, and infinite scroll
- Inline thought cards with a mood-colored bar and a rich tooltip for full content
- Discard-draft handling when resuming an unfinished entry

### Notifications and reminders

- Four independently configurable reminders:
  - **Plan** — "Start the day by planning My Day" (default on, 07:00)
  - **Review** — "Close the day with a quick review" (default on, 21:00)
  - **Schedule** — fires when timed My Day items start (default on)
  - **Check-in** — nudges when nothing has been Done for a while (default on, 30 min idle; threshold configurable 30/45/60/90/120 min)
- Separate schedulers for routines, sprints, countdowns, quick-note reminders, and daily-plan schedule reminders
- **Quiet hours / Do-Not-Disturb** policy (22:00–06:00), plus a check-in cooldown and a morning guard
- Android WorkManager workers and a foreground service for sprint notifications

### Cloud sync and backup

- **Google sign-in** (Credential Manager → Firebase Auth). Sign-in *links* a Google credential to the anonymous account, so the UID and therefore your synced data are preserved
- **Firestore sync** for **Quick Notes** (including photo attachments in Firebase Storage) and **Nested List documents**, with last-write-wins merge, debounced push on edit, pull on app resume and network reconnect, batched writes (400/batch), exponential backoff, and an explicit sync status banner
- **Android-only JSON backup** to a user-picked SAF folder (`CheckIt_Backup.json`, overwritten each run): settings plus all entity rows, excluding derived rollups. A daily WorkManager job is scheduled automatically when a folder is set (skipped if the last success was under 23 h ago), plus a manual **Back up now**
- **Restore** replaces all local data from a backup file, behind an explicit confirmation dialog

### Settings

- **Language**: English, Tiếng Việt (Vietnamese), 中文 (Chinese)
- **Theme**: System, Light, Dark
- **Color scheme**: Sunset, Sky Blue, System default
- **Account**: sign in with Google / sign out
- **Reminders** sub-screen with live summary, time pickers, and idle-threshold options
- **Backup** section (Android): folder select / change / clear, last-success time, back up now, restore from folder
- **Guidelines folder**: select / change / clear the markdown source folder
- **Dev options**: rebuild reflect rollups, force check-in reminder (bypasses quiet hours and cooldown)

### Android home-screen widgets

Three Glance widgets, each with deep links into the app:

- **Daily Plan Agenda** (4×3) — progress header, four action buttons, the top 5 quick notes, today's routines with check state, and the daily plan split into all-day vs timed with the current and next items highlighted
- **Quick Note** (4×1) — the most recent quick note's text, or today's date when empty
- **Nested Focus** (4×3) — a configurable high-priority view of a chosen nested document, with a document + max-depth picker and per-depth color coding

---

## Platform support

| Target | Notes |
| --- | --- |
| **Android** | `minSdk 28`, `compileSdk/targetSdk 37`, JVM 17. Widgets, WorkManager reminders, SAF backup, Firebase sync, Credential Manager sign-in |
| **iOS** | `iosArm64` + `iosSimulatorArm64` dynamic framework. Swift shell only; Firestore sync for Quick Notes is currently Android/macOS only |
| **macOS** | `macosArm64` dynamic framework with a native Swift app shell. Includes Swift-side Firestore sync for Quick Notes and nested documents, Google sign-in, notifications, and countdowns |

---

## Tech stack

- **Kotlin Multiplatform** + **Compose Multiplatform**, shared UI in `shared`
- **Navigation 3** (`AppRoute` sealed hierarchy) for app and settings navigation
- **Room (KMP, `room3`)** for persistence — 25 entities, 2 DAOs, schema version 7 with explicit migrations 1→7, bundled SQLite driver
- **DataStore** for preferences, **Koin** for DI, **Paging 3** for period goal history
- **Firebase** for Auth, Firestore, Storage, Analytics, and Crashlytics (Android/macOS)
- **Glance** for widgets, **WorkManager** for scheduling, **Coroutines/Flow** throughout
- **69 common tests** in `shared/src/commonTest` covering domain logic, use cases, data/sync, and ViewModels — no platform-specific test source sets
- Markdown editing/rendering built in-house (`MarkdownTextField`, `MarkdownToolbar`, `MarkdownView`), including markdown flattening for Glance widgets

---

## Project Structure

```text
CheckIt/
├── shared/        KMP module: all Compose UI, domain logic, and persistence
│   └── src/{commonMain, androidMain, iosMain, macosMain, commonTest}
├── androidApp/    Thin Android shell (manifest, MainActivity, services, res)
└── appleApp/      Xcode project (NOT a Gradle module) with two targets
    ├── iosApp/    iOSApp.swift + ContentView.swift
    └── macosApp/  Native macOS shell, Firestore sync, sign-in, notifications
```

Package layout under `shared/src/commonMain/kotlin/com/checkit/`: `auth`, `data`, `domain` (+ `domain/usecase`), `infrastructure`, `notifications`, `platform`, and `ui` (with `calendar`, `components`, `guidelines`, `journal`, `localization`, `myday`, `nested`, `quicknote`, `reflect`, `settings`, `tasks` (+ `list`, `tag`, `views`), `theme`).

---

## Development

### Prerequisites

- JDK 17+
- Android SDK (compileSdk 37, minSdk 28, targetSdk 37)
- Xcode 15+ (for iOS and macOS)

### Useful Verification Commands

```shell
# Android debug APK
./gradlew :androidApp:assembleDebug

# Shared tests (all targets)
./gradlew :shared:allTests

# Shared tests, Android host only (fastest)
./gradlew :shared:testAndroidHostTest

# iOS simulator framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# macOS app build
xcodebuild -project appleApp/appleApp.xcodeproj -scheme macosApp -configuration Debug build

# macOS release archive
xcodebuild -project appleApp/appleApp.xcodeproj -scheme macosApp -configuration Release archive -archivePath build/macosApp.xcarchive
open build/macosApp.xcarchive/Products/Applications/
```

---

## GitHub Release APK

Releases are tag-driven. Pushing any Git tag starts the GitHub Actions workflow (`.github/workflows/release-apk.yml`), builds a signed Android APK, and attaches it to a GitHub Release.

The workflow builds an APK only; it does not build an Android App Bundle (`.aab`).

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

### Local Release Build

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

---

## Notes on "AI"

CheckIt has **no LLM or cloud inference**. There is no network call anywhere in the shared code — every scheduling and suggestion feature runs on-device:

- **Smart Schedule** is a local heuristic: it learns the modal (start, end) time range per tag from the last 3 days of completed daily-plan items, then assigns slots with a conflict-aware greedy pass that schedules the most-constrained item first.
- **Add-to-My-Day suggestions** combine tag heuristics with repeat-rule and do-date logic (`AutoAddTodayTasksToMyDayUseCase`).
- **Quick-add bars** (including the one styled with a rotating "AI glow") are plain text capture. The only parsing applied is inline `#tag` extraction with fuzzy matching (`TagParser`).
- **Goal autocomplete** suggests lines from the current period goal.

If you add a real model-backed feature later, update this section.

---

## License

See [LICENSE](LICENSE).