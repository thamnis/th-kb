# Changelog

All notable changes to **th-kb** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.0-alpha.3] - 2026-11-17

### Added
- **On-Device Personal Dictionary & Adaptive Learning (`PersonalDictionaryManager`)**: Learns frequently typed custom words, names, slang, and 2-word transitions locally on-device—syncing live into both native Rust (`PersonalModel`) and Kotlin fallback with zero cloud calls.
- **Long-Press Suggestion Blacklist**: Long-pressing any non-literal suggestion in the top strip hides/blacklists it immediately so unwanted words never reappear unless restored.
- **Settings Personal Dictionary UI**: Added controls in `SettingsActivity` to add custom words manually, remove learned entries, restore hidden suggestions, or clear learned history.
- **Quoted `"Literal"` Strip Slot**: Typing a word that differs from top prediction (or an unknown custom word) now displays `“typed”` on the left slot (`[“bonj” | bonjour | bon]`), letting you tap to keep and immediately learn your exact spelling.
- **Expanded Conversational Bigrams & Trigrams**: Added dozens of high-frequency everyday French and English phrase transitions in `fr_common.json` and `en_common.json` for stronger next-word predictions.

### Changed
- Updated `ROADMAP.md` checking off all `v0.5` Text Suggestion, Dictionary, Rust Engine, and Personal Learning deliverables.

## [0.5.0-alpha.2] - 2026-11-17

### Added
- **Auto-Text Replacement & Cancel-on-Delete**: Pressing Space or punctuation (`.,!?`) automatically restores missing accents (*mogo* → *môgô*, *ecole* → *école*) and fixes adjacent-key typos in `advanced_autocomplete` mode without hijacking valid dictionary words; tapping **Delete (`⌫`)** immediately after an auto-replacement or suggestion tap cancels the replacement and restores your exact typed prefix (suppressing duplicate re-correction on the next Space).
- **Native Rust SymSpell + 2D Spatial Engine**: Compiled `libth_kb_core.so` (`arm64-v8a`, `armeabi-v7a`, `x86_64`) via Android NDK + JNI with sorted 1/2-char prefix buckets, log10 Zipf frequency scaling, SymSpell distance-1 delete index, and QWERTY/AZERTY adjacent-key proximity weighting (with graceful Kotlin fallback for JVM unit tests and non-NDK builds).
- **Real 30,000-Word EN & FR Conversational Dictionaries**: Bundled cleaned 30k English (`en_words.txt`) and French (`fr_words.txt`) wordlists alongside expanded Nouchi (`fr_ci_nouchi.json`) with balanced frequencies and canonical city names.
- **Native Cross-Compile Script**: Added `scripts/build_rust_android.sh` to build and strip Rust `.so` binaries for Android ABIs.

### Changed
- **Clean Start & Dynamic Case Alignment**: Empty text fields no longer display filler stopwords (`je – *est* – tu`)—keeping tools visible until typing starts and omitting random unigrams after spaces when no contextual bigram/trigram matches. All lookups normalize to lowercase/accent-stripped internally and format results cleanly to match user casing (`LOWER`, `TITLE`, `ALL_CAPS`).
- **Centered Top Prediction Slot**: Suggestion strip now places the highest-confidence `#1` prediction in the highlighted center slot (`[2nd, 1st, 3rd]`).

## [0.5.0-alpha.1] - 2026-11-17

### Added
- **Text Suggestion & Autocompletion Engine**: Integrated native suggestion engine in both Rust (`rust-core/src/suggestion.rs`) and Kotlin (`SuggestionEngine.kt`) featuring exact prefix lookup, case/accent insensitivity, and sub-microsecond query execution.
- **Context Window Sweet-Spot (Trigram / Bigram Context)**: Evaluates up to 96 characters of preceding text before cursor, extracting 2 context words to boost prediction accuracy (>85-90%) and offer next-word predictions before typing.
- **Ivorian French & Nouchi Dictionary (`fr_CI`)**: Curated 50+ high-frequency Ivorian words and popular street slang (*môgô*, *djossi*, *enjaillement*, *sciencer*, *chap-chap*, *gaou*, *drap*, *chicoter*, *ya fohi*, *garba*, *attiéké*, *alloco*, *kpakpato*, etc.) with colloquial bigrams/trigrams (*ya fohi*, *c'est kpata*, *il faut sciencer*).
- **Flexible Autocomplete Mode Setting**: Toggle between `{autocomplete or advanced_autocomplete}` in Settings:
  - `autocomplete`: Standard exact dictionary prefix completion.
  - `advanced_autocomplete`: Contextual Trigram/Bigram boosting, Levenshtein edit distance typo correction ($\le 1$), and next-word suggestions.
- **Dynamic 3-Candidate Suggestion Strip**: Sleek top toolbar row displaying 3 suggestion candidates with divider bars, highlighted center recommendation, single-tap word completion, and instant toggle (`>` / `<`) between suggestions and tools.
- **Simple Mode Backlogged to `v3.0`**: Created roadmap milestone for a minimalist, distraction-free pure typing mode in `ROADMAP.md`.

## [0.4.0-alpha.1] - 2026-11-17

### Added
- **Dedicated Numeric-Only Layout (Numpad)**: Added 3x4 tactile keypad (`layouts/numeric.json`) with phone keypad layout (`1-9`, `0`), mathematical operators (`+`, `-`, `*`, `/`, `=`, `%`), punctuation (`.`, `,`), and long-press variants (scientific notation, parentheses, exponentiation). Automatically selected for numeric input fields and manually togglable.
- **Glide-Up Mode Toggle for Numeric Numpad**: Gliding up on the mode button (`?123` / `ABC`) instantly toggles between alphanumeric/symbol layers and the dedicated numeric keypad, complete with visual corner hints (`123` on mode button in alpha/symbol mode, `?#` in numeric mode).
- **Compact Undo & Redo Toolbar Widget**: Sleek, unlabelled pill widget with vertical bar divider (`↺ | ↻`) in the top toolbar, spring animations on touch, and direct `performContextMenuAction` undo/redo dispatch.
- **Timestamped JSON Backup & Restore**: Added one-tap "Export Settings & History" generating ISO timestamped files (`thkb_backup_yyyyMMdd_HHmmss.json`) covering settings, clipboard items, favorites, and to-do tasks, with full validation and one-tap restore in Settings.

### Changed
- **Roadmap Organization**: Backlogged Google Tasks sync for To-Do List and Media Controller to `v1.2` release milestone in `ROADMAP.md`.
- **Numeric Variation Field Scoping**: Refined input context detection so text URIs and numeric password variations don't collide.

### Added
- **Built-in Animated Todo List Panel**: Accessible directly from the keyboard toolbar with persistent JSON storage (`TodoListManager`), task completion progress bar, animated task reordering (`animateItem()`), strike-through sweep on completion, one-tap text insertion into active apps, and delete/clear actions.
- **Gboard-Style Bottom Emoji & Kaomoji Navigation**: Moved layout navigation to the bottom row (`ABC` | `☺` | `★` | `:-)` | `⌫`) with animated indicator pills, smooth horizontal sliding and fading content transitions (`AnimatedContent`), and direct keyboard return.
- **Comprehensive Emojis Library**: Added complete Unicode sections for **Flags** (256 national, regional, and symbolic flags), **Symbols** (203+ hearts, signs, media/audio controls, arrows, zodiac, math, currencies, and geometric shapes), and **Travel & Places** (120+ vehicles, landmarks, and landscapes).
- **Two-Way Synchronized Category Navigation**: Real-time synchronization between the emoji grid scroll position and top category symbol icons (`derivedStateOf`); automatically scrolls the category bar to keep the active section in view, and smoothly scrolls to section headers on category tap.
- **Gliding Bubble Passing Transitions in Accent Popups**: Floating diacritic selection bubble spring-glides continuously (`animateFloatAsState`) beneath the user's finger as they navigate between letter variants in `AccentVariantsPopup`. Key preview bubble also glides smoothly between adjacent keys.
- **Natural Vertical Sheet Transitions**: Keyboard ↔ Emoji/Kaomoji panel switching now uses a physical vertical sliding sheet transition.
- **Expanded Intelligence & Gadgets Backlog**: Added GitHub Issue #34 (Google Tasks sync for Todo list) and logged backlog milestones for offline dictionary, voice typing, rambler, language level metrics, OCR, undo-redo, currency converter, media control, lyrics, fancy text, and note taker.

### Changed
- **Blink-Free Layer Switching**: Removed row-level blinking animations in `TouchKeyGrid` so switching between `ABC`, `?123`, and `=\<` is instant, crisp, and responsive.
- **Removed Emoji Search Bar**: Streamlined top emoji header to focus purely on quick category symbols navigation without clutter.

## [0.2.0-alpha.1] - 2026-11-16

### Added
- **Power Delete Gestures on `Backspace`**: Glide down on `Backspace` toggles between Left-Delete (`⌫`) and Right-Delete (`⌦`); from either mode, single tap deletes 1 char, very-long-press (`580ms`) starts rapid delete (`35ms` repeat), glide left enters word-by-word delete mode, and glide up deletes one full paragraph/line at once.
- **Context-Aware Typing**: Automatic `EditorInfo` detection for `NORMAL`, `URL` (`/` quick key), `EMAIL` (`@` quick key), `NUMBER` (opens in `?123`), `PASSWORD`, and `TERMINAL` (scrollable `TerminalQuickBar` with `CTRL` modifier, `ESC`, `TAB`, carets `^ < >`, arrows, and shell symbols).
- **Auto-Capitalization**: Automatically activates `ShiftState.ONCE` after `.` / `!` / `?` + `Space` and after `Return` (`\n`) in normal text fields.
- **2D Spacebar Cursor Glide**: Gliding left/right on `Space` moves the cursor by character; gliding up/down moves the cursor line-by-line (`DPAD_UP` / `DPAD_DOWN`).
- **Multi-Item Clipboard History**: Tracks up to 25 clipboard entries (`ClipboardHistoryManager`) with one-tap insert, Pin/Unpin, Delete, Clear unpinned, and `Paste from system` (with vertical resizing gated to `> 4` items).
- **High-DPI Native Compose Emoji Picker & Favorites**: Replaced `EmojiPickerView` with a native Compose `LazyVerticalGrid` (`emojis.json`), bright `#E3E3E3` section headers, category filter chips, `★ Favorites` tab (starring emojis and kaomojis), and a configurable **Emoji grid columns** setting (`6`, `7`, `8`, `9`).
- **Delete & Long-Press Settings + Usage Guide**: Added configurable normal key long-press delay (`350ms` / `450ms` / `600ms`), rapid-delete delay (`450ms` / `580ms` / `750ms`), gesture toggles, and comprehensive [`docs/USAGE.md`](docs/USAGE.md).

### Fixed
- `InputConnection` stabilization (`finishComposingText()` before commits/deletions/cursor movement and `InputType.TYPE_NULL` raw key event fallback for terminal apps).
- Cancelled pending normal key long-press popups when the finger glides before the delay expires.

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
