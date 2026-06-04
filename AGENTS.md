# AGENTS.md - CheckIt

## Project

CheckIt is a Kotlin Multiplatform calendar and task management app targeting Android and iOS, with shared Compose UI in `shared`.

## Verification Commands

```bash
# Android app compile/package check
./gradlew :androidApp:assembleDebug

# Shared KMP tests
./gradlew :shared:allTests

# Shared Android host unit tests
./gradlew :shared:testAndroidHostTest

# iOS simulator framework check
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Do not run the full `./gradlew build` by default. Use the smallest relevant task set for the files changed.

## Verification

- Docs-only changes: no build required.
- Shared/business-logic changes: run `./gradlew :shared:testAndroidHostTest`; use `./gradlew :shared:allTests` when cross-target coverage is needed.
- Android-impacting changes, including shared Compose UI: run `./gradlew :androidApp:assembleDebug`.
- iOS-specific or KMP framework changes: run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`.
- If a requested verification task cannot run, report the failure and what completed successfully.

## Testing Strategy

- Prefer many fast unit tests for edge cases and logic branches.
- Add integration tests when repository, DAO, service, or DI wiring behavior changes.
- Keep end-to-end tests limited to the most critical user flows.
- Add or update tests when behavior changes, bugs are fixed, or meaningful branches are introduced.
- Prefer fakes over mocks.

## Coding Conventions

- Keep changes minimal and scoped; avoid unrelated refactors.
- Re-read target files before editing to avoid stale patch/context mistakes.
- Platform actual file naming: `{Name}.{platform}.kt`.
- Add new dependencies in `gradle/libs.versions.toml` first, then consume via the version catalog.
- Use suspend functions and coroutines for async work.

## Architecture

- Follow UDF/MVI-style state: immutable UI state plus explicit events/actions.
- Keep ViewModels platform-light; put business logic in use cases.
- Depend on interfaces in domain; bind implementations in DI modules.
- Model loading, error, and success states explicitly.
- Use structured concurrency (`viewModelScope`, supervisor boundaries, cancellation-aware code).
- Keep persistence and networking behind repositories; avoid direct DAO/network calls from UI.

## Git

Commit only when explicitly asked.
