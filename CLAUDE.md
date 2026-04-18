# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (minified via ProGuard)
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTestCoverage  # Coverage report
```

Run a single test class:
```bash
./gradlew test --tests "com.finnflow.FullyQualifiedClassName"
```

## Architecture

MVVM + Clean Architecture, single-module Android app (`com.finnflow`).

**Data flow:** Compose UI → ViewModel (StateFlow/Flow) → Repository → Room DAO → SQLite

**Layers:**
- `data/db/` — Room database, DAOs, entities, type converters, `DatabaseSeeder` for initial data
- `data/model/` — Domain models: `Transaction`, `Category`, `SubCategory`
- `data/repository/` — Repository interfaces + implementations (injected via Hilt)
- `di/` — Hilt modules (`DatabaseModule`, `RepositoryModule`); all repos are `@Singleton`
- `ui/<feature>/` — One screen + one ViewModel per feature; ViewModels expose `StateFlow`
- `Navigation.kt` — Single `NavHost` wiring all Compose destinations

**Key constraints:**
- Min SDK 26, Target SDK 35, Java 17, Kotlin 2.0.21
- KSP (not kapt) for Hilt and Room annotation processing
- `fallbackToDestructiveMigration()` is used — schema changes will wipe data in dev builds
- `fromAccountId`/`toAccountId` columns on `transactions` are reserved (null) for a future accounts feature

## Testing

- Unit tests in `app/src/test/` use JUnit4 + MockK; async assertions use Turbine
- Instrumented tests use `HiltTestRunner` and Room in-memory database
- Test coverage is configured for debug builds only
