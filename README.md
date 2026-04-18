# FinnFlow

FinnFlow is a personal finance tracker for Android. It lets you log income and expenses, organize spending by category, and visualize where your money goes — all stored locally with no account required.

## Features

- **Transaction tracking** — Log income, expenses, and transfers with amount, date, category, subcategory, and notes
- **Categories & subcategories** — 23 built-in categories (Food & Dining, Housing, Health, Salary, etc.) with subcategories; fully customizable
- **Monthly dashboard** — Daily grouped transaction history with income, expense, and balance totals; navigate across months
- **Stats & charts** — Donut chart with category breakdown, percentages, and transaction counts; filter by income or expense
- **Yearly overview** — Month-by-month income/expense table for the full year
- **Offline-first** — All data stored locally via Room/SQLite; no sync, no account needed

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

## Build

```bash
./gradlew assembleDebug     # Debug APK
./gradlew assembleRelease   # Release APK (minified via ProGuard)
```

## Test

```bash
./gradlew test                          # Unit tests
./gradlew connectedAndroidTest          # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTestCoverage     # Coverage report
```

Run a single test class:

```bash
./gradlew test --tests "com.finnflow.FullyQualifiedClassName"
```
