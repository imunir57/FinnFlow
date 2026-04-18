# FinnFlow

A personal finance Android app built with Jetpack Compose, Room, and Hilt.

## Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| State | ViewModel + StateFlow |
| DI | Hilt |
| Database | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Charts | Vico |
| Testing | JUnit4 · MockK · Turbine · Room in-memory |

## Project structure

```
app/src/main/java/com/finnflow/
├── data/
│   ├── db/           # AppDatabase, Converters, DAOs, Entities
│   ├── model/        # Domain models (Transaction, Category, SubCategory)
│   └── repository/   # Repository interfaces + implementations
├── di/               # Hilt modules (DatabaseModule, RepositoryModule)
├── ui/
│   ├── components/   # BottomNavBar
│   ├── home/         # HomeScreen + HomeViewModel
│   ├── stats/        # StatsScreen + StatsViewModel
│   ├── transaction/  # TransactionFormScreen + TransactionViewModel
│   ├── category/     # CategoryScreen + SubCategoryScreen + CategoryViewModel
│   ├── yearly/       # YearlyScreen + YearlyViewModel
│   └── theme/        # Material 3 theme
├── MainActivity.kt
├── FinnFlowApp.kt
└── Navigation.kt
```

## Running tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Test coverage report
./gradlew testDebugUnitTestCoverage
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Database schema (v1)

```
transactions  ──FK──▶  categories  ◀──FK──  sub_categories
    id                     id                    id
    type                   name                  categoryId
    amount                 type                  name
    date                   iconName
    categoryId             colorHex
    subCategoryId (null)
    note
    fromAccountId (null)   ← reserved for account integration
    toAccountId   (null)   ← reserved for account integration
```

## Roadmap

See [CHANGELOG.md](CHANGELOG.md) — Unreleased section.
