# Design Implementation Backlog

Design reference files: `finnflow-design/project/screen-*.jsx` — one JSX file per screen.

---

## 1. Settings screen — `screen-settings.jsx`

**Changes to `ui/settings/SettingsScreen.kt`:**

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

## 2. Categories screen — `screen-categories.jsx`

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

## Design system reference

All JSX components are in `finnflow-design/project/`:
- `data.jsx` — mock data, `fmt()`, `groupByDate()`, `aggregateByCategory()`, colour palette
- `icons.jsx` — 50+ SVG icons (stroke-based, 24×24, `currentColor`)
- `frame.jsx` — device shell, StatusBar, BottomNav, FAB components
- `screen-home.jsx`, `screen-stats.jsx`, `screen-stats-insights.jsx`, `screen-insights.jsx`
- `screen-add.jsx`, `screen-yearly.jsx`, `screen-settings.jsx`, `screen-profile.jsx`, `screen-categories.jsx`
