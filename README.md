# Minimala

A minimalist Android home launcher built with Jetpack Compose. Three swipeable panels, a
gesture-driven app drawer, and a settings screen for palette, typography, and layout — no icons,
no widgets grid, no clutter.

## Layout

Horizontal pager with three pages, home always in the center:

- **Left panel** — upcoming calendar events, quick access to system settings.
- **Home panel** — clock, date, battery, double-tap to lock the screen (via accessibility
  service), long-press for a quick-launch grid.
- **Right panel** — quick add for a calendar event, favorite apps list.
- **App drawer** — dragged up from any panel as an overlay (not a fourth page), with
  favorite/frequent/hidden toggles, rename, and app info shortcuts per app.

## Features

- Six bundled color palettes (Dark, Light, Dracula, Strawberry, Blueberry, Lemon).
- Five bundled variable-weight fonts (Inter, JetBrains Mono, Outfit, Space Grotesk, Lora) plus
  system default, with adjustable weight and scale.
- Adjustable wallpaper scrim opacity — dial the wallpaper from fully visible to fully hidden.
- Calendar integration: view upcoming events and create new ones without leaving the launcher.
- Screen-off-on-double-tap via an optional accessibility service (a launcher has no other way to
  invoke `GLOBAL_ACTION_LOCK_SCREEN` without being a device owner).
- Favorites, frequent apps, hidden apps, and per-app renaming, all persisted through DataStore.

## Stack

- Kotlin, Jetpack Compose, Material 3
- `androidx.datastore-preferences` for settings persistence
- `kotlinx.serialization` for settings/state encoding
- `kotlinx.coroutines` for async work
- JUnit + Turbine for tests

## Requirements

- Android 8.0 (API 26) or later
- Compile/target SDK 36

## Building

```bash
./gradlew assembleDebug
```

Install and set as default launcher:

```bash
./gradlew installDebug
```

Then press the device Home button and pick Minimala.

## Permissions

- `READ_CALENDAR` / `WRITE_CALENDAR` — for the calendar panel and quick event creation.
- `EXPAND_STATUS_BAR` — swipe down on the home panel to reveal notifications/quick settings.
- Accessibility service (optional, user-enabled) — double-tap to turn the screen off.
