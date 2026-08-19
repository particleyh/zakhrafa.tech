# Zakhrafa (زخرفة) - Engineering Standards

## Website (html/)
- **CSS:** Always use `/css/style.css`. Avoid inline styles.
- **JS Engine:** Core logic lives in `/js/zakhrafa-engine.js`.
- **UI Logic:** Common UI interactions (generate, copy, results grid) live in `/js/ui.js`.
- **Pages:** Sub-pages (arabic, pubg, etc.) should use the same shared assets to ensure consistency.

## Android Apps (app/)
- **Architecture:** Multi-module Gradle project.
  - `:engine`: Shared Kotlin library for decoration logic.
  - `:app`: Decorator application (Jetpack Compose).
  - `:keyboard`: Keyboard application (InputMethodService).
- **Tech Stack:** Kotlin 2.0, Compose, Material 3, JDK 21.
- **Standards:**
  - Use `libs.versions.toml` for dependency management.
  - Keep the engine pure and platform-agnostic where possible.
  - UI should be responsive and support RTL by default.

## Deployment
- **Web:** Nginx root is `/var/www/zakhrafa.tech/html`.
- **Android:** Build using `./gradlew assembleRelease`. Sign using `apksigner`.
