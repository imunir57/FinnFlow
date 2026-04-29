# Changelog

All notable changes to **FinnFlow** are documented here.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added

**Profile & onboarding**
- First-launch onboarding screen — name input with "Get Started" and "Skip" options; skipping still marks onboarding as complete
- Local user profile persisted via DataStore Preferences (`displayName`, `initials`, `hasCompletedOnboarding`)
- `UserProfileRepository` interface + `UserProfileRepositoryImpl` backed by DataStore
- `ProfileModule` — Hilt `@Provides` for singleton `DataStore<Preferences>` and `@Binds` for the repo
- `ProfileScreen` — avatar initials, editable display name, Google sign-in stub (future sync/backup)
- `MainViewModel` — exposes `hasCompletedOnboarding: StateFlow<Boolean?>` (`null` = loading) to gate the start destination
- `MainNavHost` updated: blank frame while DataStore is loading, then routes to Onboarding or Home based on flag; bottom bar hidden on non-tab screens

**Settings screen**
- `SettingsScreen` — list-based settings page replacing the direct category navigation; items: Categories, Profile, Currency, Backup, About
- Back button on Settings screen
- Tapping the avatar/initials on Home navigates directly to Profile

**App icon**
- Custom adaptive icon across all mipmap densities (mdpi → xxxhdpi) using `app_icon.png`
- Solid white `ic_launcher_background` replacing the default Android vector

**Transaction form**
- Date picker dialog (Material3 `DatePickerDialog`) wired to the date field on `TransactionFormScreen`

**Stats screen**
- Custom date range navigation: tapping the date chips in stats opens a date picker for `CUSTOM` period selection

**Tests**
- `UserProfileRepositoryTest` — 9 unit tests: Flow mapping (defaults, name, two-word/single-word initials, onboarding flag), `saveProfile` trimming, `updateData` delegation, `completeOnboarding` flag write, `clearProfile`
- `OnboardingViewModelTest` — 7 unit tests: get-started with/without name, skip, blank/empty name guards, `navigateHome` channel emissions, consecutive calls
- `ProfileViewModelTest` — 5 unit tests: default initial state, repo data reflection, multi-emission, `saveName` delegation
- `MainViewModelTest` — 4 unit tests: resolved non-null value, false/true onboarding flag, latest-value reflection

### Changed

**Warm UI theme (YearlyScreen)**
- `YearlyScreen` summary card redesigned to match `HomeScreen` hero card — dark gradient, Taka watermark, income/expense/net in a single row per month
- Month names use full form only (no abbreviations)

**HomeScreen**
- Settings icon replaces the 3-dot menu button; taps navigate to `SettingsScreen`
- Avatar initials and greeting name now driven by `UserProfile` from DataStore (no longer hardcoded)
- `HomeViewModel` injects `UserProfileRepository`; `HomeUiState` exposes `displayName` and `initials`

**HomeViewModel**
- `getAllCategories()` combined into the main `uiState` flow so category names render without a separate fetch

**Bottom navigation**
- Settings removed from `BottomNavBar`; now 3 tabs only: Home, Stats, Yearly

### Fixed

- Amount validation no longer shows an error on initial form load before the user has typed anything

---

> Features planned but not yet released.

- Backup & restore (local file export / import)
- Account integration (bank accounts, wallets)
- Transfer between accounts
- Recurring transaction support
- Budget / spending limits per category
- Widget for home screen balance
- Google Sign-In for cross-device sync and backup

---

## [1.0.0-alpha.2] — 2025-04-20

### Added

**Stats screen redesign**
- `StatsPeriod` — removed `DAILY`; now `MONTHLY`, `ANNUALLY`, `CUSTOM` only
- Unified control bar: period chips (Month / Year / Custom) and Income/Expense toggle on the same row separated by a vertical divider
- Donut chart with `%` labels rendered inside each slice via `android.graphics.Paint` on `Canvas`; labels suppressed on slices narrower than 25° to avoid cramping
- Category list rows show colour bar, name, transaction count, percentage, and amount; tap navigates to `CategoryDetailScreen`
- `StatsViewModel` rewritten: `QueryParams` internal state drives `flatMapLatest` so any param change triggers a single reactive re-fetch; exposes `currentFrom`, `currentTo`, `currentType` for nav arg passthrough

**Category detail screen**
- `CategoryDetailScreen` — donut chart (category-specific shade ramp), subcategory list, inline transaction expansion
- Tapping any subcategory row toggles an `AnimatedVisibility` panel showing individual transactions indented below the row; chevron icon (▲/▼) reflects state
- `CategoryDetailViewModel` — loads `SubCategorySummary` reactively; transactions fetched lazily on first expand and cached in `Map<Long?, List<Transaction>>` to avoid re-querying on collapse/re-expand
- `CategoryDetail` nav route added: `stats/category/{categoryId}/{from}/{to}/{type}`

**Data layer**
- `SubCategorySummary` domain model added to `Models.kt`
- `TransactionDao.getSubCategorySummary()` — `LEFT JOIN sub_categories` so transactions without a subcategory appear as `Uncategorised`; grouped by `subCategoryId`, ordered by `totalAmount DESC`
- `TransactionDao.getTransactionsBySubCategory()` — handles `NULL` subcategory via `(:subCategoryId IS NULL AND subCategoryId IS NULL) OR subCategoryId = :subCategoryId`
- `TransactionRepository` interface and impl extended with both new methods

**Seed data on first launch**
- `SeedData` — defines 18 default categories (12 expense, 5 income, 1 transfer) with 70+ subcategories
- `DatabaseSeeder` — `RoomDatabase.Callback` that inserts seed data inside `onCreate` via a background coroutine; uses `Provider<AppDatabase>` to avoid circular Hilt dependency
- `AppModule` updated — `DatabaseSeeder` registered via `.addCallback()` on the Room builder

**Tests**
- `StatsViewModelTest` — updated: removed `DAILY` test, added `noDailyOption_inStatsPeriod`, `percentOf` accuracy, zero-total guard, exposed property checks (8 tests total)
- `CategoryDetailViewModelTest` — 8 tests: initial load, expand/collapse, row switching, cache hit verification, `percentOf`, category name loading
- `TransactionDaoStatsTest` — 11 instrumented tests for the two new DAO queries: grouping, `NULL` subcat, ordering, date range, cross-category isolation, type filtering
- `TransactionRepositoryStatsTest` — 6 unit tests: delegation, domain mapping, null passthrough, multi-entity mapping
- `SeedDataTest` (unit) — 11 tests covering uniqueness, blank names, hex color format, type counts, subcategory completeness
- `DatabaseSeederTest` (instrumented) — 7 tests verifying seed writes correctly to in-memory Room: category count, type breakdown, subcategory counts, color/type preservation

---

## [1.0.0] — 2025-04-15 — Initial Setup

### Added

**Project structure**
- Android project scaffolded with Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)
- Version catalog (`gradle/libs.versions.toml`) for centralised dependency management
- Hilt dependency injection configured (`@HiltAndroidApp`, `DatabaseModule`, `RepositoryModule`)
- Material 3 dynamic colour theme with light/dark support

**Data layer**
- `AppDatabase` (Room v1) with `exportSchema = true`
- `Converters` — `LocalDate` ↔ `String`, `TransactionType` ↔ `String`
- Entities: `TransactionEntity`, `CategoryEntity`, `SubCategoryEntity`
- Foreign keys: SubCategory → Category (CASCADE delete), Transaction → Category (RESTRICT), Transaction → SubCategory (SET NULL)
- `TransactionDao` — insert, update, delete, `getByMonth`, `getByDateRange`, `getMonthlyTotalsByYear`, `getCategorySummary`
- `CategoryDao` — full CRUD for categories and sub-categories, Flow-returning queries

**Domain layer**
- Domain models: `Transaction`, `Category`, `SubCategory`, `CategoryWithSubCategories`, `CategorySummary`
- `TransactionType` enum: `INCOME`, `EXPENSE`, `TRANSFER`
- Repository interfaces: `TransactionRepository`, `CategoryRepository`
- Implementations: `TransactionRepositoryImpl`, `CategoryRepositoryImpl`

**UI layer (Jetpack Compose)**
- `MainActivity` with edge-to-edge enabled
- `MainNavHost` — `NavHost` with `Scaffold` + `BottomNavBar`
- Navigation routes: `Screen` sealed class covering all destinations
- `BottomNavBar` — Home / Stats / Yearly / Settings with `saveState` / `restoreState`
- `HomeScreen` — fixed `TopAppBar` (month selector + 3-dot menu), `SummaryBar` (income / expense / balance), `LazyColumn` of daily-grouped transactions with edit/delete per item, FAB for add
- `TransactionFormScreen` — type selector chips, amount field with validation, date field, category dropdown, sub-category dropdown (conditional), note field, save button
- `StatsScreen` — period tabs (Daily / Monthly / Annually / Custom), income/expense toggle, chart/list toggle, `LinearProgressIndicator` per category, Vico chart placeholder
- `YearlyScreen` — year navigation, annual summary card, 12-month breakdown list
- `CategoryScreen` — category list with edit/delete, FAB to add, navigate to sub-category management
- `SubCategoryScreen` — sub-category list with edit/delete/add per parent category

**ViewModels**
- `HomeViewModel` — `selectedMonth` state, `getTransactionsByMonth` Flow, income/expense totals, daily groups map
- `TransactionViewModel` — form state, validation (`amountError`, `isValid`), add vs edit routing via `SavedStateHandle`
- `StatsViewModel` — period/date range management, `combine` income + expense flows, view toggle
- `YearlyViewModel` — year navigation, `flatMapLatest` on year state
- `CategoryViewModel` — category or sub-category CRUD depending on `SavedStateHandle`

**Test coverage**
- `TransactionDaoTest` — insert, getByMonth filter, delete, update, dateRange filter, categorySummary aggregation (in-memory Room)
- `CategoryDaoTest` — category CRUD, type filter, cascade delete to sub-categories, sub-category insert
- `TransactionRepositoryTest` — all methods mocked with MockK + Turbine Flow assertions
- `CategoryRepositoryTest` — all methods mocked with MockK + Turbine Flow assertions
- `HomeViewModelTest` — initial load, daily groups, month navigation, delete delegation
- `TransactionViewModelTest` — category load, amount validation, isValid guard, add/update routing, type change reset
- `StatsViewModelTest` — initial load, type switch, view toggle, period change, custom range
- `YearlyViewModelTest` — initial load, year navigation, monthly data mapping
- `CategoryViewModelTest` — category vs sub-category mode, CRUD delegation, no-parent guard
- `ConvertersTest` — LocalDate and TransactionType round-trips
- `HiltTestRunner` for instrumented test suite

---

## How to update this file

When you complete a feature or fix, add an entry under `[Unreleased]` using the categories below.  
On release, rename `[Unreleased]` to the version + date and open a new empty `[Unreleased]` section.

| Category | When to use |
|---|---|
| **Added** | New feature or file |
| **Changed** | Change to existing behaviour |
| **Deprecated** | Soon-to-be-removed feature |
| **Removed** | Removed feature |
| **Fixed** | Bug fix |
| **Security** | Security-related fix |

---

[Unreleased]: https://github.com/your-org/FinnFlow/compare/v1.0.0...HEAD
[1.0.0-alpha.2]: https://github.com/your-org/FinnFlow/compare/v1.0.0...v1.0.0-alpha.2
[1.0.0]: https://github.com/your-org/FinnFlow/releases/tag/v1.0.0
