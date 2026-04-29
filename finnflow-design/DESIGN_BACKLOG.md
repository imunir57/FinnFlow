# Design Implementation Backlog

Design reference files: `finnflow-design/project/screen-*.jsx` — one JSX file per screen.

Implement in this order (easiest → most complex):

---

## 1. Yearly screen — `screen-yearly.jsx`

**Changes to `ui/yearly/YearlyScreen.kt` + `YearlyViewModel.kt`:**

- **Monthly averages strip** — two pill cells below the hero card: "Avg / month in" (green) and "Avg / month out" (clay)
- **Column header row** — "Month" | "In · Out · Net" header above the monthly list
- **Per-month progress bars** — two thin bars (income and expense) below each month's amounts, normalized to the year's max value; no bars on zero-data months
- **"Now" badge** — dark pill badge beside the current month name
- **Background tint** — light wash (`WarmCard`) on the current month's row

---

## 2. Stats screen — `screen-stats.jsx`

**Changes to `ui/stats/StatsScreen.kt`:**

- **"View Insights" button** — appears below the donut chart on Expense view only
  - trending-up icon + "Insights" label + short description + chevron right
  - Taps navigate to new Insights screen (add `Screen.Insights` route)

---

## 3. Settings screen — `screen-settings.jsx`

**Full redesign of `ui/settings/SettingsScreen.kt`:**

- **Profile card at top** (replaces plain "Profile" row):
  - Avatar (50px circle, green, initials) + name + "Tap to edit profile" + chevron
  - Clickable → navigates to Profile
- **Section headers** — uppercase grouped labels: "Manage", "Data", "App"
- **Icon badge per row** — every row gets a 34×34px icon badge (use category-style coloured squares)
- **New rows — Manage section:**
  - Categories (bag icon)
  - Currency (coin icon) — right-aligned current symbol
  - Notifications (bell icon) — "Daily reminder · 9:00 PM" + ON/OFF toggle
- **New rows — Data section:**
  - Backup (cloud-up icon) — "Last backup — [date]"
  - Restore (cloud-down icon) — "From a previous backup file"
  - Export CSV (export icon) — "Share your transactions as a spreadsheet"
- **New rows — App section:**
  - Appearance (palette icon) — right: "System"
  - App Lock (lock icon) — "Require fingerprint to open" + OFF toggle
  - About (info icon) — "Version 1.0.0 · Build 102"
- **Sign Out button** — full width, `ExpenseClay` text, light border, below all sections
- **Footer** — italic serif: *"FinnFlow · made for keeping count"*

---

## 4. Profile screen — `screen-profile.jsx`

**Changes to `ui/profile/ProfileScreen.kt` + `ProfileViewModel.kt`:**

- **Larger avatar** — 90×90px; pencil icon button (32px) overlaid at bottom-right
- **Inline name editing** — tapping the pencil replaces the name `Text` with a bottom-border-only `BasicTextField`; Enter/check saves, Escape cancels; no separate Save button card
- **Stats triptych card** — three cells separated by thin vertical rules:
  - Income (currency, green) | Expense (currency, clay) | Entries (count)
  - Serif numbers (18sp), uppercase labels (10sp)
  - ViewModel needs to expose these totals — either from `HomeUiState` or a new query
- **Account section:**
  - Email row (mail icon + email string)
  - Cloud Sync row (cloud icon + "Sign in with Google" + "SOON" badge)
  - Privacy row (shield icon + "Data stored on device only")
- **Preferences section:**
  - Start of Month row (calendar icon + "1st")
  - Default Currency row (coin icon + symbol)
- **Footer** — "Member since [date from DataStore or hardcoded Jan 2025]"

---

## 5. Categories screen — `screen-categories.jsx`

**Changes to `ui/category/CategoryScreen.kt` + `CategoryViewModel.kt`:**

- **Drag-to-reorder handle** — three horizontal lines (≡) on the left of each row; use `LazyColumn` with `reorderable` library or manual drag state
- **Sub-count pill** — small rounded badge showing subcategory count, right of name
- **Sub-category preview** — first 3 sub names as secondary text, truncated with "…"
- **Info box** — dashed-border panel at list bottom: info icon + "Categories with existing transactions can't be deleted…"
- **Bottom-sheet editor** (`CategoryEditSheet`):
  - Semi-transparent backdrop overlay
  - Drag handle at top
  - Icon picker grid — 8 columns, 16 icon options; selected = coloured border + light bg
  - Colour swatch grid — 13 colours, circular; selected = checkmark + ring
  - Delete button (clay colour, trash icon) — only when editing existing category
  - Cancel / Save side-by-side at bottom
  - Replaces the current dialog/inline editing

---

## 6. Add Transaction screen — `screen-add.jsx`

**Full redesign of `ui/transaction/TransactionFormScreen.kt`:**

- **Date chips** (replaces date picker field) — 4-chip grid:
  - Today (chip with date below), Yesterday, [3 days ago with weekday], Pick (calendar icon)
  - Active chip: dark background, light text; inactive: light border
- **Embedded numeric keypad** (replaces system keyboard for amount):
  - 3×4 grid: digits 1–9, then [calc icon | 0 | backspace]
  - 50px height buttons, rounded (12px), serif font for digits
- **Calculator overlay** (full-screen, triggered by calc key):
  - Expression display (small monospace, right-aligned)
  - Result display (large serif with currency symbol)
  - Cancel / "Use Amount" buttons
  - 4×5 button grid: operators (÷ × − + =), functions (C ⌫ ( )), digits + decimal
- **Category pills with icons** (replaces `DropdownMenu`):
  - Flex-wrap row; each pill: icon badge (20px) + category name
  - Active: coloured border + light bg tint; inactive: light border
- **Sub-category pills** (replaces dropdown):
  - Text-only rounded pills in flex-wrap row

---

## 7. Insights screen — `screen-insights.jsx` + `screen-stats-insights.jsx` *(new screen)*

**New files: `ui/insights/InsightsScreen.kt` + `InsightsViewModel.kt`**

Add `Screen.Insights` route; navigate from Stats screen "View Insights" button.

- **Savings-rate hero card** (dark gradient, same as Home/Yearly):
  - Large percentage (56sp serif) with % sign
  - Dynamic message:
    - ≥30%: "Strong month…"
    - ≥10%: "Net ৳X saved so far…"
    - ≥0%: "Thin margin…"
    - <0%: "Overspent by ৳X…"

- **Income vs Expense bar card:**
  - Two stacked horizontal bars (income green, expense clay), height 8px
  - Label + amount (monospace) left of each bar
  - Net balance row below
  - Caption: "Saved X% of income" or "Overspent by X%"

- **Daily spend trend card:**
  - SVG sparkline: gradient fill (clay fading to transparent), line, dotted average line
  - Peak-day dot (with stroke), today dot (solid)
  - X-axis labels: first / mid / current / last day of month
  - Caption: "Avg ৳X / day · N days recorded"
  - ViewModel needs `List<Pair<LocalDate, Double>>` of daily totals for the month

- **Highlights card** — three rows:
  - Biggest single transaction (amount + category + date)
  - Most frequent category (name + count)
  - Biggest MoM change (category + delta %)

- **Day-of-week heatmap card:**
  - 7 columns Sun–Sat; each column: amount label + 32×32px coloured square + day abbrev
  - Square intensity based on spend (darkest = peak day)
  - Peak day: border + bold label
  - ViewModel needs spending aggregated by `DayOfWeek`

---

## Design system reference

All JSX components are in `finnflow-design/project/`:
- `data.jsx` — mock data, `fmt()`, `groupByDate()`, `aggregateByCategory()`, colour palette
- `icons.jsx` — 50+ SVG icons (stroke-based, 24×24, `currentColor`)
- `frame.jsx` — device shell, StatusBar, BottomNav, FAB components
- `screen-home.jsx`, `screen-stats.jsx`, `screen-stats-insights.jsx`, `screen-insights.jsx`
- `screen-add.jsx`, `screen-yearly.jsx`, `screen-settings.jsx`, `screen-profile.jsx`, `screen-categories.jsx`
