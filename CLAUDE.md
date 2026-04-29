# CLAUDE.md

Guidance for Claude Code when working in this repository.

---

## Build commands

```bash
./gradlew assembleDebug                  # Debug APK
./gradlew assembleRelease                # Release APK (minified via ProGuard)
./gradlew test                           # All unit tests
./gradlew connectedAndroidTest           # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTestCoverage      # Coverage report (debug builds only)
./gradlew test --tests "com.finnflow.ui.HomeViewModelTest"  # Single class
```

---

## Architecture

MVVM + Clean Architecture, single-module Android app (`com.finnflow`).

**Data flow:** Compose UI → ViewModel (`StateFlow`) → Repository → Room DAO → SQLite  
**Profile/prefs flow:** Compose UI → ViewModel (`StateFlow`) → Repository → DataStore Preferences

**Key constraints:**
- Min SDK 26, Target SDK 35, Java 17, Kotlin 2.0.21
- KSP (not kapt) for Hilt and Room annotation processing
- `fallbackToDestructiveMigration()` — schema changes wipe data in dev builds
- `fromAccountId`/`toAccountId` on `transactions` table are reserved (null) for a future accounts feature
- Packaging exclusions for `META-INF/LICENSE.md`, `LICENSE-notice.md`, `NOTICE.md` (MockK transitive conflict)

---

## Folder structure

```
app/src/main/java/com/finnflow/
│
├── FinnFlowApp.kt                  @HiltAndroidApp entry point
├── MainActivity.kt                 Single activity, edge-to-edge, calls MainNavHost
│
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt          Room DB (v1); exports schema; registers DatabaseSeeder
│   │   ├── Converters.kt           LocalDate ↔ String, TransactionType ↔ String
│   │   ├── DatabaseSeeder.kt       RoomDatabase.Callback; seeds on first onCreate
│   │   ├── SeedData.kt             18 categories, 70+ subcategories, static seed lists
│   │   ├── dao/
│   │   │   ├── CategoryDao.kt      CRUD for categories + subcategories; Flow-returning queries
│   │   │   └── TransactionDao.kt   getByMonth, getByDateRange, getCategorySummary,
│   │   │                           getSubCategorySummary, getTransactionsBySubCategory,
│   │   │                           getMonthlyTotalsByYear
│   │   └── entity/
│   │       └── Entities.kt         TransactionEntity, CategoryEntity, SubCategoryEntity
│   │
│   ├── model/
│   │   └── Models.kt               Domain models: Transaction, Category, SubCategory,
│   │                               CategoryWithSubCategories, CategorySummary, SubCategorySummary
│   │
│   ├── profile/                    DataStore-backed user profile (no Room)
│   │   ├── UserProfile.kt          data class: displayName, initials, hasCompletedOnboarding
│   │   ├── UserProfileRepository.kt  interface: profile Flow, saveProfile, completeOnboarding, clearProfile
│   │   └── UserProfileRepositoryImpl.kt  DataStore<Preferences> impl; keys: profile_display_name,
│   │                                     onboarding_completed; edit() calls updateData() internally
│   │
│   └── repository/
│       ├── CategoryRepository.kt        interface
│       ├── CategoryRepositoryImpl.kt    delegates to CategoryDao
│       ├── TransactionRepository.kt     interface
│       └── TransactionRepositoryImpl.kt delegates to TransactionDao
│
├── di/
│   ├── AppModule.kt        DatabaseModule (object): DB, DAOs
│   │                       RepositoryModule (abstract class): @Binds for Transaction + Category repos
│   └── ProfileModule.kt    DataStoreModule (object): singleton DataStore<Preferences>
│                           ProfileModule (abstract class): @Binds UserProfileRepository
│
└── ui/
    ├── Navigation.kt       Screen sealed class — all route strings in one place
    ├── MainNavHost.kt      NavHost + Scaffold; onboarding gate via MainViewModel;
    │                       bottom bar hidden on non-tab routes (settings, profile, categories…)
    ├── MainViewModel.kt    Exposes hasCompletedOnboarding: StateFlow<Boolean?> (null = loading)
    │
    ├── components/
    │   └── BottomNavBar.kt 3 tabs: Home, Stats, Yearly (Settings removed from nav bar)
    │
    ├── home/
    │   ├── HomeScreen.kt       Avatar (tappable → Profile), greeting, hero balance card,
    │   │                       month navigator, daily-grouped transaction list, FAB
    │   └── HomeViewModel.kt    Combines transactions + categories + profile into HomeUiState
    │
    ├── onboarding/
    │   ├── OnboardingScreen.kt Hero card, name input, Get Started, Skip, Google coming-soon note
    │   └── OnboardingViewModel.kt  onGetStarted(name), onSkip() → Channel<Unit> navigateHome
    │
    ├── profile/
    │   ├── ProfileScreen.kt    Avatar initials, editable name, Google stub, back button
    │   └── ProfileViewModel.kt profile: StateFlow<UserProfile>, saveName(), signInWithGoogle() stub
    │
    ├── settings/
    │   └── SettingsScreen.kt   Back button + list: Categories, Profile, Currency, Backup, About
    │
    ├── stats/
    │   ├── StatsScreen.kt          Period tabs, type toggle, donut chart, category list
    │   ├── StatsViewModel.kt       QueryParams drives flatMapLatest; exposes currentFrom/To/Type
    │   ├── CategoryDetailScreen.kt Subcategory breakdown, inline transaction expansion
    │   └── CategoryDetailViewModel.kt  Lazy-load + cache transactions per subcategory
    │
    ├── yearly/
    │   ├── YearlyScreen.kt     Year nav, hero card (dark gradient), monthly income/expense/net rows
    │   └── YearlyViewModel.kt  flatMapLatest on year state
    │
    ├── transaction/
    │   ├── TransactionFormScreen.kt  Type chips, amount, date, category + subcategory dropdowns, note
    │   └── TransactionViewModel.kt   Form state, validation, add vs edit via SavedStateHandle
    │
    ├── category/
    │   ├── CategoryScreen.kt       List with edit/delete, FAB; navigate to SubCategoryScreen
    │   └── CategoryViewModel.kt    Category or sub-category CRUD based on SavedStateHandle
    │
    └── theme/
        ├── Color.kt    See palette below
        └── Theme.kt    MaterialTheme wrapper
```

---

## Navigation routes (`Navigation.kt`)

| Object | Route |
|---|---|
| `Screen.Home` | `home` |
| `Screen.Stats` | `stats` |
| `Screen.Yearly` | `yearly` |
| `Screen.Settings` | `settings` |
| `Screen.Onboarding` | `onboarding` |
| `Screen.Profile` | `profile` |
| `Screen.AddTransaction` | `transaction/add` |
| `Screen.EditTransaction` | `transaction/edit/{transactionId}` |
| `Screen.Categories` | `categories` |
| `Screen.SubCategories` | `subcategories/{categoryId}` |
| `Screen.CategoryDetail` | `stats/category/{categoryId}/{from}/{to}/{type}` |

Bottom bar shows on: `home`, `stats`, `yearly` only. All other routes hide it.

---

## Theme / design tokens (`ui/theme/Color.kt`)

| Token | Hex | Use |
|---|---|---|
| `WarmPaper` | `#FAF9F6` | Screen background |
| `WarmCard` | `#F2EFE9` | Card / sheet surface |
| `WarmSurface` | `#EDE9E2` | Slightly deeper surface |
| `Ink` | `#28221E` | Primary text |
| `InkMedium` | `#6B6056` | Secondary text, icons |
| `InkFaint` | `#A89B8F` | Placeholder, captions |
| `Rule` | `#DDD8D0` | Dividers, borders |
| `IncomeGreen` | `#2D6B41` | Income amounts, avatar bg |
| `ExpenseClay` | `#B5452B` | Expense amounts |

**Hero card gradient** (dark, used on Home, Yearly, Onboarding):
```kotlin
Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF1A2820),
        0.4f to Color(0xFF1E1916),
        1.0f to Color(0xFF241410)
    )
)
```

---

## Hilt DI patterns

**Object module** (`@Provides`): use for things that need construction logic (DB, DAOs, DataStore).  
**Abstract class module** (`@Binds`): use for binding interface → impl. Must be separate from object module.

```kotlin
// Pattern used in AppModule.kt and ProfileModule.kt
@Module @InstallIn(SingletonComponent::class)
object FooModule {
    @Provides @Singleton fun provideFoo(...): Foo = ...
}

@Module @InstallIn(SingletonComponent::class)
abstract class FooBindings {
    @Binds abstract fun bindFooRepo(impl: FooRepoImpl): FooRepo
}
```

All repositories are `@Singleton`. DataStore is `@Singleton` via `preferencesDataStore` delegate on `Context`.

---

## ViewModel pattern

```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val repo: FooRepository
) : ViewModel() {

    val uiState: StateFlow<FooUiState> = repo.someFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FooUiState())

    fun doSomething() {
        viewModelScope.launch { repo.doSomething() }
    }
}
```

For navigation events from ViewModel (e.g. onboarding → home):
```kotlin
private val _navigateHome = Channel<Unit>(Channel.BUFFERED)
val navigateHome = _navigateHome.receiveAsFlow()
// Collect in the composable with LaunchedEffect + collectLatest
```

For combining 3+ flows, use a named data class instead of destructuring a Triple:
```kotlin
private data class TxData(val month: YearMonth, val txs: List<Transaction>, ...)
val uiState = combine(flow1, flow2, flow3) { txData, cats, profile -> ... }
```

---

## Testing structure

```
app/src/test/java/com/finnflow/
├── data/
│   ├── ConvertersTest.kt                   Simple unit tests, no mocking
│   ├── dao/
│   │   ├── CategoryDaoTest.kt              In-memory Room (Robolectric)
│   │   ├── TransactionDaoTest.kt           In-memory Room (Robolectric)
│   │   └── TransactionDaoStatsTest.kt      In-memory Room — subcategory queries
│   ├── db/
│   │   └── SeedDataTest.kt                 Unit tests for seed data integrity
│   ├── profile/
│   │   └── UserProfileRepositoryTest.kt    Mocks DataStore<Preferences>; tests Flow mapping
│   │                                       and updateData delegation
│   └── repository/
│       ├── CategoryRepositoryTest.kt        Mocks CategoryDao
│       ├── TransactionRepositoryTest.kt     Mocks TransactionDao
│       └── TransactionRepositoryStatsTest.kt
│
└── ui/
    ├── HomeViewModelTest.kt
    ├── MainViewModelTest.kt
    ├── StatsViewModelTest.kt
    ├── YearlyViewModelTest.kt
    ├── TransactionViewModelTest.kt
    ├── CategoryViewModelTest.kt
    ├── CategoryDetailViewModelTest.kt
    ├── onboarding/
    │   └── OnboardingViewModelTest.kt
    └── profile/
        └── ProfileViewModelTest.kt

app/src/androidTest/java/com/finnflow/
├── HiltTestRunner.kt               Custom runner (currently commented out)
└── ui/
    └── DatabaseSeederTest.kt       Instrumented Room test for seeder
```

### Unit test template

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FooRepository   // mockk(relaxed = true)
    private lateinit var vm: FooViewModel

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.someFlow() } returns flowOf(someData)
        vm = FooViewModel(repo)
    }
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun someState_reflectsRepo() = runTest {
        vm.uiState.test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun action_delegatesToRepo() = runTest {
        vm.doSomething()
        coVerify { repo.doSomething() }
    }
}
```

### Key testing rules
- Always `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@Before`, reset in `@After`
- Use `runTest { }` for any coroutine-based assertion
- Use Turbine `.test { }` for Flow/StateFlow assertions; always end with `cancelAndIgnoreRemainingEvents()`
- Mock repositories with `mockk(relaxed = true)` to reduce boilerplate
- For DataStore: mock `DataStore<Preferences>`; `edit { }` calls `updateData` internally — mock `updateData` to capture transforms
- `SavedStateHandle(mapOf("key" to value))` to pass nav arguments into ViewModels under test
- Prefer `coVerify` for suspend functions, `verify` for regular functions

---

## Features implemented (as of branch `feature/profile-onboarding`)

| Feature | Files |
|---|---|
| Home screen | `ui/home/HomeScreen.kt`, `HomeViewModel.kt` |
| Transaction form (add/edit) | `ui/transaction/TransactionFormScreen.kt`, `TransactionViewModel.kt` |
| Stats + donut chart | `ui/stats/StatsScreen.kt`, `StatsViewModel.kt` |
| Category detail breakdown | `ui/stats/CategoryDetailScreen.kt`, `CategoryDetailViewModel.kt` |
| Yearly overview | `ui/yearly/YearlyScreen.kt`, `YearlyViewModel.kt` |
| Category & subcategory CRUD | `ui/category/CategoryScreen.kt`, `CategoryViewModel.kt` |
| Settings screen (list) | `ui/settings/SettingsScreen.kt` |
| Onboarding (first launch) | `ui/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt` |
| Local user profile | `ui/profile/ProfileScreen.kt`, `ProfileViewModel.kt`, `data/profile/` |
| Seed data on first install | `data/db/DatabaseSeeder.kt`, `SeedData.kt` |
| Adaptive app icon | `res/mipmap-*/ic_launcher.png`, `res/drawable/ic_launcher_foreground.png` |

