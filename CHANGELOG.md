# Changelog

All notable changes to **th-kb** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0-alpha.1] - 2026-11-16

### Added
- `v0.1` Gboard-parity IME core: `ThKbInputMethodService` with `LifecycleOwner`, `SavedStateRegistryOwner`, and `ViewModelStoreOwner` bridge, dynamic `EditorInfo.imeOptions` return action, subtype-aware layout switching (`en_US` / `fr_FR`), haptic/sound key feedback, paste action, selection-aware backspace (`getSelectedText`), and spacebar cursor glide (`setSelection` with `DPAD_LEFT/RIGHT` fallback).
- Raw `pointerInput` (`awaitEachGesture`) multi-touch key grid (`KeyboardRoot`) with `44dp` top toolbar, `4 × 52dp` key rows, single-tap/double-tap 3-state Shift with fillable `Canvas` arrow icon, tap/hold Delete, `?123` / `=\<` symbol layers, instant 2-thumb rollover, and zero dead-zone inter-key hit-testing.
- Gboard-style 1-row and 2-row floating long-press popups with `68.dp` unclipped top headroom (`onComputeInsets`), finger-locked selection cursor, `.` and `,` punctuation grids, and comprehensive symbol/number/currency/accent variants (`accents.json`).
- Unified Emoji & Kaomoji panel (`androidx.emoji2` `EmojiPickerView` + `Kaomoji (^_^)` tabs) with floating tap/hold Backspace button and vertical drag/expand resizing (`236.dp`–`460.dp`).
- `KbPreferences` and `SettingsActivity` for layout selection (`qwerty_en`, `azerty_fr`), auto-Shift on keyboard open (`autoCapOnOpen`), haptic feedback, and key-press sound toggles.
- Initial repository structure for `th-kb` (`com.thamnis.thkb`) with Kotlin/Jetpack Compose Android IME module (`app/`) and Rust native core (`rust-core/`) using `uniffi-rs`.
- Project governance and documentation: `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, Apache-2.0 `LICENSE`, `.editorconfig`, `.gitignore`, and GitHub Actions CI workflow.

### Fixed
- Enabled `WindowCompat.setDecorFitsSystemWindows(win, false)` and placed `navigationBarsPadding()` + `10.dp` bottom elevation inside the dark `Surface` so the bottom system dismiss/IME-switcher bar is cleanly reserved and filled on Pixel 7a.
- Switched `TouchKeyGrid` hit-testing to `localBoundingBoxOf` scoped per layout/layer (`remember(rows)`) and wrapped state callbacks in `rememberUpdatedState` so long-press and key taps work immediately on first open.
- Formatted `rust-core/src/search.rs` with `rustfmt` so GitHub Actions CI passes both Rust and Android jobs.
