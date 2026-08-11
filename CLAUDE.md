# CLAUDE.md

Guidance for Claude Code when working in this repository.

---

## Build commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest           # requires device/emulator — DAO tests live here
./gradlew test --tests "com.finnflow.ui.HomeViewModelTest"
```

Google Sign-In needs `GOOGLE_WEB_CLIENT_ID=<web-client-id>.apps.googleusercontent.com` in
`local.properties` (gitignored). Builds and unit tests succeed without it — the build logs a
warning and sign-in returns a "not configured" error at runtime. Never make a non-auth feature
depend on it.

---

## Architecture

MVVM + Clean Architecture, single-module Android app (`com.finnflow`).

**Data flow:** Compose UI → ViewModel (`StateFlow`) → Repository → Room DAO → SQLite
**Profile/settings flow:** Compose UI → ViewModel (`StateFlow`) → `UserProfileRepository` → DataStore Preferences

**Key constraints:**
- Min SDK 26, Target SDK / compile SDK 35, Java 17, Kotlin 2.0.21, AGP 8.5.2
- KSP (not kapt) for Hilt and Room annotation processing
- Room schema `version = 4`, no destructive-migration fallback — every schema change must bump the
  version and register a `Migration` in `AppDatabase.MIGRATIONS` (schemas exported to `app/schemas`),
  with a `MigrationTestHelper` test under `src/androidTest`
- `fromAccountId`/`toAccountId` on `transactions` table reserved (null) for future accounts feature
- Categories and sub-categories are **archived, never deleted** (`isArchived`). `transactions`
  references both, so deleting either fails outright (`categoryId` is ON DELETE RESTRICT) or
  rewrites history (`subCategoryId` is ON DELETE SET NULL). Picker queries
  (`getCategoriesByType`, `getActiveSubCategories`) exclude archived rows; lookup queries
  (`getAllCategories`, `getAllSubCategories`) must keep returning them so past transactions stay
  labelled, and Stats keeps reporting them wherever they have amounts
- Category and sub-category lists are ordered by `sortOrder ASC, name ASC`. Reordering rewrites
  every position in the list at once (`CategoryDao.setCategoryOrder` / `setSubCategoryOrder`) —
  a partial write would interleave with the rows still at the default 0
- Packaging exclusions for `META-INF/LICENSE.md`, `LICENSE-notice.md`, `NOTICE.md` (MockK conflict)
- `buildConfig = true`; release build is minified via ProGuard

---

## Folder structure

```
app/src/main/java/com/finnflow/
├── data/
│   ├── auth/         GoogleAuthClient (+ Impl) — Credential Manager sign-in
│   ├── biometric/    BiometricAuthenticator (+ Impl) — app lock gate
│   ├── db/           AppDatabase, DatabaseSeeder, SeedData, Converters
│   │   ├── dao/      TransactionDao, CategoryDao
│   │   └── entity/   Entities.kt
│   ├── model/        Models.kt — domain models; Currency.kt — ISO 4217 enum
│   ├── notification/ ReminderScheduler (+ Impl), DailyReminderWorker (WorkManager)
│   ├── profile/      UserProfile, UserProfileRepository (+ Impl) — DataStore
│   └── repository/   TransactionRepository, CategoryRepository, BackupRepository (+ Impl), CsvExporter
├── di/               AppModule.kt, ProfileModule.kt, SettingsModule.kt, AuthModule.kt
└── ui/
    ├── Navigation.kt, MainNavHost.kt, MainViewModel.kt
    ├── home/, stats/, insights/, yearly/, transaction/, category/,
    │   settings/, onboarding/, profile/, lock/
    ├── components/   BottomNavBar.kt, ConfirmationDialog.kt, OptionPickerSheet.kt
    └── theme/        Color.kt, Theme.kt
```

Each screen folder has `*Screen.kt` + `*ViewModel.kt`. Exceptions: `settings/` also holds
`AboutScreen.kt` (no ViewModel), `category/` holds `SubCategoryScreen` inside `CategoryScreen.kt`.

---

## Navigation routes (`Navigation.kt`)

| Object | Route |
|---|---|
| `Screen.Home` | `home` |
| `Screen.Stats` | `stats` |
| `Screen.Yearly` | `yearly` |
| `Screen.Insights` | `insights` |
| `Screen.Settings` | `settings` |
| `Screen.About` | `about` |
| `Screen.Onboarding` | `onboarding` |
| `Screen.Profile` | `profile` |
| `Screen.AddTransaction` | `transaction/add` |
| `Screen.EditTransaction` | `transaction/edit/{transactionId}` |
| `Screen.Categories` | `categories` |
| `Screen.SubCategories` | `subcategories/{categoryId}` |
| `Screen.CategoryDetail` | `stats/category/{categoryId}/{from}/{to}/{type}` |

Bottom bar shows on: `home`, `stats`, `yearly` only.

`MainNavHost` gates the whole graph: it returns early while `hasCompletedOnboarding` is `null`
(DataStore still loading), then renders `AppLockScreen` instead of the `NavHost` when app lock is
enabled and the session is not yet unlocked. Start destination is `home` or `onboarding`.

---

## Theme / design tokens (`ui/theme/Color.kt`)

Light and dark schemes both exist; `FinnFlowTheme(darkTheme = …)` picks between them. The theme
mode (`system` / `light` / `dark`) is resolved in `MainNavHost` from the user profile — pass it
explicitly rather than relying on the `isSystemInDarkTheme()` default.

| Light token | Hex | Dark counterpart | Hex | Use |
|---|---|---|---|---|
| `WarmPaper` | `#FAF9F6` | `DarkPaper` | `#1B1815` | Screen background |
| `WarmCard` | `#F2EFE9` | `DarkCard` | `#262220` | Card / sheet surface |
| `WarmSurface` | `#EDE9E2` | `DarkSurface` | `#2E2926` | Raised surface |
| `Ink` | `#28221E` | `IvoryInk` | `#F3EFE9` | Primary text |
| `InkMedium` | `#6B6056` | `IvoryInkMedium` | `#C7BFB5` | Secondary text, icons |
| `InkFaint` | `#A89B8F` | `IvoryInkFaint` | `#8C8377` | Placeholder, captions |
| `Rule` | `#DDD8D0` | `DarkRule` | `#3A342F` | Dividers, borders |
| `IncomeGreen` | `#2D6B41` | `IncomeGreenDark` | `#6FCB8C` | Income amounts, avatar bg |
| `ExpenseClay` | `#B5452B` | `ExpenseClayDark` | `#E08469` | Expense amounts |

**Hero card gradient** (dark in both themes; used on Home, Yearly, Insights, Onboarding):
```kotlin
Brush.linearGradient(colorStops = arrayOf(
    0.0f to Color(0xFF1A2820), 0.4f to Color(0xFF1E1916), 1.0f to Color(0xFF241410)
))
```

---

## Hilt DI patterns

`@Provides` in `object` module for construction logic; `@Binds` in separate `abstract class` module
for interface → impl binding. Must be separate Kotlin classes — `AppModule.kt` holds both
(`DatabaseModule` object + `RepositoryModule` abstract class) as the reference example.

All repositories `@Singleton`. DataStore `@Singleton` via `preferencesDataStore` delegate on
`Context`. `DatabaseSeeder` receives a `Provider<AppDatabase>` to break the circular dependency
with the database it seeds.

---

## ViewModel pattern

```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(private val repo: FooRepository) : ViewModel() {
    val uiState = repo.someFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FooUiState())

    fun doSomething() { viewModelScope.launch { repo.doSomething() } }
}
```

One-shot navigation events: `Channel<Unit>(Channel.BUFFERED).receiveAsFlow()` — collect with
`LaunchedEffect + collectLatest`.

Combining 3+ flows: use a named private data class (not Triple) to avoid destructuring issues.

Use `null` as the "still loading" state for DataStore-backed flags so the UI can hold off rendering
(see `MainViewModel.hasCompletedOnboarding`).

---

## Testing

Unit tests: `app/src/test/java/com/finnflow/` — mirrors `data/` and `ui/` structure.
Instrumented: `app/src/androidTest/` — DAO tests (`CategoryDaoTest`, `TransactionDaoTest`,
`TransactionDaoStatsTest`), `DatabaseSeederTest.kt`, `HiltTestRunner.kt`. DAO tests run on device,
not in the JVM suite — don't add Room DAO tests under `src/test`.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(testDispatcher); repo = mockk(relaxed = true) }
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun state_reflectsRepo() = runTest {
        vm.uiState.test { assertEquals(expected, awaitItem()); cancelAndIgnoreRemainingEvents() }
    }
}
```

**Rules:**
- `UnconfinedTestDispatcher` + `setMain/resetMain` always
- Turbine `.test { }` for all Flow assertions; end with `cancelAndIgnoreRemainingEvents()`
- `mockk(relaxed = true)` for repos; `coVerify` for suspend, `verify` for regular
- DataStore: mock `updateData` (not `edit`); use `slot` to capture and execute the transform
- `SavedStateHandle(mapOf("key" to value))` for nav arg ViewModels

---

## Features implemented

Home, Transaction form, Stats + donut, Category detail, Insights, Yearly, Category CRUD (icon /
colour picker), Settings, About, Onboarding, Local user profile (DataStore), Google Sign-In via
Credential Manager, Currency selection, Light/dark/system theme, Daily reminder notifications
(WorkManager), App lock (biometric), CSV export, JSON backup & restore, Seed data, Adaptive app icon.

Design backlog: `finnflow-design/DESIGN_BACKLOG.md`
