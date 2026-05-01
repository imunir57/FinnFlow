---
name: implement-screen
description: Implement a FinnFlow screen from the design backlog. Use when the user says "implement X screen", "build the Y screen", or "start on design backlog item N".
allowed-tools: Read Glob Grep Edit Write Bash Agent TodoWrite
---

# implement-screen

Implement a FinnFlow screen from design files and the backlog. Argument: the screen name (e.g. `yearly`, `settings`, `profile`, `categories`, `stats`, `add`, `insights`).

---

## Step 1 — Gather design context (run in parallel)

1. Read `finnflow-design/DESIGN_BACKLOG.md` — find the section for `$ARGUMENTS` and extract every bullet point listed under it.
2. Read the matching design file: `finnflow-design/project/screen-$ARGUMENTS.jsx` (e.g. `screen-yearly.jsx`). For stats insights, also read `screen-stats-insights.jsx`.
3. Read `finnflow-design/project/data.jsx` — colour palette and `fmt()` helper.

---

## Step 2 — Read existing implementation files (run in parallel)

Find and read the current Kotlin files for this screen:

- `app/src/main/java/com/finnflow/ui/$ARGUMENTS/*Screen.kt`
- `app/src/main/java/com/finnflow/ui/$ARGUMENTS/*ViewModel.kt`

Also read one nearby screen for convention reference (e.g. `ui/yearly/` or `ui/home/`) to understand composable structure, spacing, and colour token usage.

---

## Step 3 — Read supporting files if ViewModel changes are needed

If the backlog item requires new data (new queries, new StateFlow fields):

- Read the relevant DAO: `app/src/main/java/com/finnflow/data/db/dao/*.kt`
- Read the relevant repository interface and impl
- Read `app/src/main/java/com/finnflow/data/model/Models.kt`

---

## Step 4 — Plan before coding

List the changes required based on backlog + JSX, grouped by file. Confirm the plan with the user before writing any code. Keep the list short and concrete (one line per change).

---

## Step 5 — Implement

Apply changes file by file. Follow these conventions strictly:

**Kotlin / Compose conventions:**
- Background: `WarmPaper`, cards: `WarmCard`, primary text: `Ink`, secondary: `InkMedium`, captions: `InkFaint`, dividers: `Rule`
- Income amounts: `IncomeGreen`, expense amounts: `ExpenseClay`
- Hero card dark gradient: `Brush.linearGradient(colorStops = arrayOf(0.0f to Color(0xFF1A2820), 0.4f to Color(0xFF1E1916), 1.0f to Color(0xFF241410)))`
- `MaterialTheme.typography` for type; serif feel via `FontFamily.Serif` on hero numbers
- No hardcoded colours — always use tokens from `Color.kt`
- No hardcoded strings except labels that will never be translated
- `@HiltViewModel` + `@Inject constructor` for all ViewModels
- StateFlow via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultState())`
- One-shot events: `Channel<Unit>(Channel.BUFFERED).receiveAsFlow()`
- Combining 3+ flows: use a named private `data class` (not Triple)
- No comments unless the WHY is non-obvious

**Navigation:**
- New routes go in `Navigation.kt` as `object NewScreen : Screen("route")`
- Wire in `MainNavHost.kt`
- Bottom bar only shows on `home`, `stats`, `yearly`

---

## Step 6 — Write tests

After implementation, write or update unit tests:

**Test file location:** `app/src/test/java/com/finnflow/ui/$ARGUMENTS/` or the matching `data/` path.

**Template to follow:**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: FooRepository

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        every { repo.someFlow() } returns flowOf(testData)
    }
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun state_reflectsRepo() = runTest {
        val vm = FooViewModel(repo)
        vm.uiState.test {
            assertEquals(expected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

**Rules:**
- `UnconfinedTestDispatcher` + `setMain/resetMain` always
- Turbine `.test { }` for every Flow assertion; always end with `cancelAndIgnoreRemainingEvents()`
- `mockk(relaxed = true)` for repos
- `coVerify` for suspend calls, `verify` for regular
- DataStore: mock `updateData` (not `edit`); use `slot` to capture and execute the transform lambda
- Cover: initial state, state updates from repo, each public action, edge cases (empty list, zero values)

---

## Step 7 — Verify

Run `./gradlew test --tests "com.finnflow.*$ARGUMENTS*"` and fix any failures before reporting done.

---

## Step 8 — Summary

Report:
- Files changed (with line counts)
- New tests added (test names)
- Anything deferred or not yet implemented (with reason)