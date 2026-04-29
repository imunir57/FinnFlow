# CLAUDE.md

Guidance for Claude Code when working in this repository.

---

## Build commands

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedAndroidTest           # requires device/emulator
./gradlew test --tests "com.finnflow.ui.HomeViewModelTest"
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
- `fromAccountId`/`toAccountId` on `transactions` table reserved (null) for future accounts feature
- Packaging exclusions for `META-INF/LICENSE.md`, `LICENSE-notice.md`, `NOTICE.md` (MockK conflict)

---

## Folder structure

```
app/src/main/java/com/finnflow/
├── data/
│   ├── db/           AppDatabase, DAOs, Entities, DatabaseSeeder, SeedData, Converters
│   ├── model/        Models.kt — domain models
│   ├── profile/      UserProfile, UserProfileRepository, UserProfileRepositoryImpl (DataStore)
│   └── repository/   TransactionRepository, CategoryRepository (+ Impl)
├── di/               AppModule.kt, ProfileModule.kt
└── ui/
    ├── Navigation.kt, MainNavHost.kt, MainViewModel.kt
    ├── home/, stats/, yearly/, transaction/, category/, settings/, onboarding/, profile/
    ├── components/   BottomNavBar.kt
    └── theme/        Color.kt, Theme.kt
```

Each screen folder has `*Screen.kt` + `*ViewModel.kt`.

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

Bottom bar shows on: `home`, `stats`, `yearly` only.

---

## Theme / design tokens (`ui/theme/Color.kt`)

| Token | Hex | Use |
|---|---|---|
| `WarmPaper` | `#FAF9F6` | Screen background |
| `WarmCard` | `#F2EFE9` | Card / sheet surface |
| `Ink` | `#28221E` | Primary text |
| `InkMedium` | `#6B6056` | Secondary text, icons |
| `InkFaint` | `#A89B8F` | Placeholder, captions |
| `Rule` | `#DDD8D0` | Dividers, borders |
| `IncomeGreen` | `#2D6B41` | Income amounts, avatar bg |
| `ExpenseClay` | `#B5452B` | Expense amounts |

**Hero card gradient** (dark, used on Home, Yearly, Onboarding):
```kotlin
Brush.linearGradient(colorStops = arrayOf(
    0.0f to Color(0xFF1A2820), 0.4f to Color(0xFF1E1916), 1.0f to Color(0xFF241410)
))
```

---

## Hilt DI patterns

`@Provides` in `object` module for construction logic; `@Binds` in separate `abstract class` module for interface → impl binding. Must be separate Kotlin classes.

All repositories `@Singleton`. DataStore `@Singleton` via `preferencesDataStore` delegate on `Context`.

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

One-shot navigation events: `Channel<Unit>(Channel.BUFFERED).receiveAsFlow()` — collect with `LaunchedEffect + collectLatest`.

Combining 3+ flows: use a named private data class (not Triple) to avoid destructuring issues.

---

## Testing

Unit tests: `app/src/test/java/com/finnflow/` — mirrors `data/` and `ui/` structure.  
Instrumented: `app/src/androidTest/` — `DatabaseSeederTest.kt`, `HiltTestRunner.kt`.

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

Home, Transaction form, Stats + donut, Category detail, Yearly, Category CRUD, Settings, Onboarding, Local user profile (DataStore), Seed data, Adaptive app icon.

Design backlog: `finnflow-design/DESIGN_BACKLOG.md`
