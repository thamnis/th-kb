# Changelog

All notable changes to **th-kb** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0-beta.1] - 2026-11-23

### Added
- **Rambler Voice Mode** (`RambleTextCleanup`, `VoiceTypingPanel`): Toggleable continuous long-form dictation with hesitation/stutter removal, spoken line/paragraph breaks, auto-capitalization, and one-tap **Clean & Insert**.
- **Language Level Metrics** (`TypingMetricsManager`, `SettingsPage.METRICS`): On-device CEFR estimation (`A1`–`C2`), vocabulary richness percentage, average word length, complex word count, and glide/voice typing stats.
- **On-Device OCR Text Scanner** (`OcrScannerActivity` + ML Kit Text Recognition): Capture via camera, gallery, or latest clipboard screenshot/image and insert recognized text directly into the active text field.
- **Fancy Text Generator** (`FancyTextTransformer`): 9 Unicode font styles (`Bold Serif`, `Italic`, `Bold Script`, `Fraktur`, `Double-Struck`, `Monospace`, `Circled`, `Small Caps`, `Fullwidth`) usable both as live typing styles and instant style previews.
- **Built-in Note Taker** (`NotesManager`): Quick multi-note scratchpad panel (`PanelMode.SUITE`) with pinning, title/body editing, and one-tap snippet insertion.
- **Currency Converter** (`CurrencyConverter`): Offline-first calculator supporting `XOF`, `EUR`, `USD`, `GBP`, `CAD`, `CHF`, `XAF`, `NGN`, `GHS`, `MAD`, `JPY`, `CNY` with optional live rate refresh and one-tap result insertion.

## [1.3.0-beta.2] - 2026-11-23

### Added
- **Media Session Sync (`ThKbMediaListenerService`)**: Optional player sync in Media Controls displays live Play vs Pause state matching the active player, track/artist title, and reliable ±15s Rewind/Fast-Forward seeking (`transportControls.seekTo` + `KEYCODE_MEDIA_SKIP_BACKWARD`/`FORWARD` fallback).
- **Screenshots in Clipboard History**: `ClipboardHistoryManager` observes `MediaStore` screenshots (with `READ_MEDIA_IMAGES` / permission prompt in Settings & Clipboard panel) and handles OEM delayed screenshot clipboard writes.
- **Full Custom Dictionary Viewer & Filler Word Cleanup**: Displays all saved & learned words (removed 30-word limit), adds search + `Most Used` / `A → Z` sorting + one-tap `"Clean filler"` button, and filters out passive common particles (`là`, `je`, `tu`, `quoi`, etc.).
- **Intuitive PC ↔ Phone Interconnection**: Remote panel now auto-scans all Wi-Fi subnet broadcasts & `224.0.0.167` multicast on open, shows live Ping connection health (`Green` connected badge), provides an inline quick IP/peer switcher right inside the keyboard, and adds `"Send Clip"` to PC + guided setup in Settings.

### Changed
- **Bigger Media Volume Steps**: `Vol --` / `Vol ++` now step `STREAM_MUSIC` by 2–4 notches (~15–20%) per tap instead of a single tiny step.

## [1.3.0-beta.1] - 2026-11-23

### Added
- **Ergonomic Keyboard Modes (`v1.1`)**:
  - **Split Thumb Mode**: Separates key rows evenly down the middle with symmetric left/right Spacebars.
  - **One-Hand Mode (Left / Right)**: Docks the keyboard to 84% width with a quick side rail to expand, flip hand, or enter Floating mode.
  - **Floating Window**: Draggable rounded floating keyboard card with persisted X/Y placement and real `onComputeInsets` (`TOUCHABLE_INSETS_REGION`) pass-through outside the keyboard card.
  - **Quick Mode & Tool Switcher**: Hold the `☺` key to reveal a 2×5 bar for Clipboard, To-Do, Navigate, Media, Remote, Normal, Split, 1-Hand, Float, and Settings.
- **Themes & Material You (`v1.1`)**:
  - Reactive theme engine (`LocalKbPalette`, `KbColorPalette`) supporting **Material You** (Android 12+ dynamic wallpaper colors), **Dark Graphite**, **Midnight AMOLED**, **Warm Paper**, **Ocean Slate**, and **Forest Sage** with live swatches in Settings.
- **Power Navigation & Rich Media (`v1.2`)**:
  - **Navigation Panel**: Move or **Select** cursor position by Letter, Word, Phrase, Paragraph, Line Home/End, and Document Start/End, plus Select All / Copy / Cut / Paste.
  - **System Media Controls**: Compact playback transport (`Play/Pause`, `Previous`, `Next`, `Rewind`, `Fast-Forward`) and Volume (`Vol-`, `Mute`, `Vol+`).
  - **Rich Media Copy-Pasting**: `ClipboardHistoryManager` caches clipboard images via `FileProvider`, previews thumbnails in `ClipboardPanel`, and commits them via `InputConnectionCompat.commitContent()`.
  - **Discord `:shortcode:` Emoji Writing**: Typing `:word` shows inline emoji completions in the suggestion strip, and closing `:` auto-replaces known shortcodes (`:fire:` → `🔥`).
- **Cross-Device File Sending & Remote Input (`v1.3`)**:
  - **LocalSend v2 & KDE Connect**: LAN Wi-Fi peer discovery (`224.0.0.167:53317` multicast & `:1716` broadcast) and file transfer client in **Settings → Cross-Device & Remote**.
  - **Remote KB & Mouse (`rust-companion`)**: Built-in touchpad, scroll rail, mouse clicks, and text-to-PC bar inside the keyboard (`PanelMode.REMOTE`), backed by the standalone `rust-companion/` UDP desktop companion crate.

## [1.0.0-beta.3] - 2026-11-20

### Added
- **Shift Case-Toggle on Selection**: Tapping `SHIFT` while words or a sentence are selected cycles the selection through `lowercase` → `Title Case` → `UPPERCASE` → `lowercase`, preserving selection and Undo history.

### Changed
- **Full-Toolbar Undo / Redo Swipe**: Removed the Undo/Redo arrow buttons pill and enabled horizontal swipe-to-Undo (`←`) / swipe-to-Redo (`→`) across the entire top toolbar space, including while suggestions are displayed.
- **Removed First-Launch Gesture Tip**: Removed the dismissible gesture tip bar above the toolbar.

### Fixed
- **Key Preview Bubble Release**: Ensured the key preview bubble disappears immediately and reliably when lifting fingers or cancelling a gesture.

## [1.0.0-beta.2] - 2026-11-20

### Added
- **Toolbar Undo / Redo Glide**: Slide horizontally across the toolbar Undo/Redo pill (`←` / `→`) to step continuously through Undo and Redo history snapshots with light haptic ticks.
- **Char-Glided Symbols & Numbers**: Character-by-character glide typing (`260ms` dwell) now works across symbol (`?123`, `=\<`) and numeric (`123`) layers when Char-by-Char Glide Mode is active.
- **First-Launch Dismissible Gesture Tip**: Added a compact, one-time dismissible tip bar above the keyboard (`Hold ☺ for tools • Glide ↑ ⇧ for char glide • Glide Undo/Redo`), persisted via `KbPreferences.setDismissedGestureTip`.
- **Offline Voice Packs Shortcut**: Added an **Offline Voice Packs** navigation row in **Settings** (`Settings.ACTION_VOICE_INPUT_SETTINGS`) to manage system speech recognition & offline voice languages.
- **Distinct Haptic Feedback Primitives**: Separated standard key taps (`KEYBOARD_TAP`), light cursor/glide ticks (`CLOCK_TICK`), and mode/gesture toggles (`LONG_PRESS`).

## [1.0.0-beta.1] - 2026-11-20

### Added
- **Dialect & Jargon Vocabulary Browser**: Tap any pack in **Settings → Dialect & Jargon Store** to inspect, search, and filter all words and multi-word expressions inside that pack (`DialectPack.allWords`, `DialectPack.samplePhrases`).
- **2D Spacebar Cursor Dot & Hold-to-Switch Delay**: Replaced spacebar cursor arrows with a sleek 2D spring-animated circular dot (`translationX` + `translationY`) and increased Spacebar language-switch hold delay to `1s`.
- **Shift Glide-Up Mode Switch**: Glide up on `SHIFT` (`⇧`) to toggle between **Word Glide** (`Word`) and **Char-by-Char Glide** (`Char`) without cluttering the toolbar.
- **Hidden Tools Under Emoji Key**: Hold the `☺` key to reveal a floating pill menu for **Clipboard**, **To-Do List**, and **Settings**; removed To-Do, Clipboard, Settings, and Emoji buttons from the main toolbar.
- **Alphanumeric Keyboard Emoji Search**: Tapping Search inside the Emoji panel now opens the full alphanumeric keyboard with a live matching emoji strip above the keys.
- **Live Voice Streaming & Extended Silence Patience**: Voice typing streams partial words live into the text field, trims oldest words from the left in the voice panel (`formatLeftTrimmedVoiceStream`), extends silence tolerance (`14s`), and stays strictly inside `th-kb` without external Google voice popups.
- **Smart Spacebar Top-1 Commit & `'` → `?` Shift Mapping**: Pressing Space commits the top-1 suggestion (cancelable via Backspace, while manually tapped suggestions stay preserved on Backspace), and `SHIFT` turns `'` into `?`.

### Changed
- **Release Optimization**: Enabled R8 minification and resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`) for release APKs.
- **Fixed Taller Emoji Panel**: Set Emoji & Kaomoji panel to a fixed taller `344.dp` height with tighter zero-gap emoji grid spacing (`32.sp` at 8 columns).
- **Fixed Symbol Layer Language Bug**: Separated active symbol/numeric layer IDs from `languageLayoutId` so entering `?123` never resets the keyboard language.

## [0.8.0-alpha.1] - 2026-11-19

### Added
- **French Contractions & Dynamic Elision Synthesis (Proposition A)**:
  - Enriched `fr_common.json` and `en_common.json` with high-frequency French and English contractions (`c'est`, `j'ai`, `n'est`, `qu'il`, `d'accord`, `aujourd'hui`, `don't`, `can't`, `I'm`, `you're`, `that's`).
  - Updated `strip_accents` / `stripAccents` and added dynamic French elision synthesis (`l'`, `d'`, `j'`, `n'`, `s'`, `c'`, `qu'`) in both Rust (`suggestion.rs`) and Kotlin (`SuggestionEngine.kt`) so typing or gliding `"lecole"`, `"cest"`, or `"jai"` produces `"l'école"`, `"c'est"`, and `"j'ai"`.
- **Double-Letter Loop & Dwell Detection in Glided Typing (Proposition B)**:
  - Added `detect_dwell_or_loop_zones` in `rust-core/src/suggestion.rs` and fixed log-frequency scaling in `score_swipe_word` so pausing or circling on a doubled key (`ll`, `nn`, `ss`, `pp`, `tt`) boosts double-letter candidates.
- **Rust-Backed Bilingual Emoji & Kaomoji Search Bar (Proposition C)**:
  - Added `search_emojis_bilingual` (`nativeSearchEmojis`) in `rust-core/src/search.rs` and an inline **`🔍` Search Bar** inside [`EmojiAndKaomojiPanel.kt`](app/src/main/kotlin/com/thamnis/thkb/ui/EmojiAndKaomojiPanel.kt) with English, French, and Ivorian/Nouchi keywords (`feu`, `coeur`, `rire`, `merci`, `garba`, `ci`) and an A–Z quick filter strip.
- **Modularized UI Architecture & Pixel 7a Voice Service Discovery + Rust PCM Analyzer (Proposition D)**:
  - Split `KeyboardRoot.kt` into [`KeyboardTheme.kt`](app/src/main/kotlin/com/thamnis/thkb/ui/KeyboardTheme.kt), [`EmojiAndKaomojiPanel.kt`](app/src/main/kotlin/com/thamnis/thkb/ui/EmojiAndKaomojiPanel.kt), [`ClipboardAndTodoPanels.kt`](app/src/main/kotlin/com/thamnis/thkb/ui/ClipboardAndTodoPanels.kt), and [`VoiceTypingPanel.kt`](app/src/main/kotlin/com/thamnis/thkb/ui/VoiceTypingPanel.kt).
  - Updated [`VoiceInputManager.kt`](app/src/main/kotlin/com/thamnis/thkb/ime/VoiceInputManager.kt) to discover accessible `RecognitionService` components (`GoogleTTSRecognitionService` / FUTO / Whisper / Sherpa) avoiding restricted Pixel `AiAiSpeechRecognitionService` UID blocks, plus added native Rust 16kHz PCM RMS analysis (`nativeAnalyzePcmRms`).

## [0.7.0-alpha.1] - 2026-11-19

### Added
- **FUTO-Inspired Glided Typing (Disappearing Comet Trail + Word, Sentence & Char-by-Char Gliding)**:
  - Native Rust swipe decoder (`decode_swipe_with_overlay`) combining inflection/corner anchor detection, Gaussian key proximity, and monotone DP trajectory alignment across EN, FR, all 14 Dialect Store packs, and personal dictionary.
  - Real-time disappearing comet trail (`175ms` decay window) that vanishes smoothly into your fingertip as you glide.
  - **Continuous Sentence Gliding**: Glide across letters and touch `SPACE` mid-glide to commit the word + space and continue gliding the next word in one unbroken gesture.
  - **Char-by-Char Glide Mode**: Toggle `Glide: Char` in the toolbar or Settings (or pause/dwell on keys while gliding) to glide-type custom words and full sentences character by character.
- **Reliable Voice Typing (`VoiceInputManager` + System Voice Fallback)**:
  - Fixed IME permission & lifecycle race so returning from the microphone permission dialog or system recognizer never closes or resets the Voice panel.
  - Uses Main-Looper `SpeechRecognizer` with automatic engine fallback and a one-tap **System Voice ↗** recognizer dialog fallback (`ACTION_RECOGNIZE_SPEECH`).
- **Deterministic IME Snapshot Undo / Redo Engine**:
  - Added in-IME delta snapshot `undoStack` and `redoStack` (`TextSnapshot`) in `ThKbInputMethodService` so Undo (`↺`) and Redo (`↻`) work reliably in 100% of apps (Jetpack Compose, EditText, WebViews, WhatsApp, Keep).
- **Animated Glide & Spacebar Cursor Feedback**:
  - Added soft luminous key-glide indication (`isGlidedOver`) that illuminates and lifts keys smoothly as your finger glides across them.
  - Added spring-animated sliding cursor thumb (`«  ▬  »`) on the spacebar that shifts left and right in real time while moving the cursor.

### Fixed
- **Alphanum Switch Button (`?123` / `ABC`)**:
  - Removed duplicate `"123"` glide-up hint on Row 2 in the symbol-only (`=\<`) layer so only the bottom-left alphanum switch button carries the glide-up indicator.
  - Fixed glide-up indicator on the Numeric Numpad (`123`) layer to display `"?#"` (and switch to Symbols on glide-up) instead of duplicating `"ABC"`.
  - Changed the bottom-left alphanum switch button shape from a full capsule pill to a rounded rectangle (`10.dp`) matching control keys.
- **Larger, Tighter Emoji Grid**:
  - Reduced inter-emoji grid spacing to `1.dp` and increased 8-column emoji font size to `28.5.sp` for bigger, clearer emojis.

## [0.6.0-alpha.1] - 2026-11-17

### Added
- **Modular Dialect & Jargon Store (`DialectPackManager` + Native Rust Overlay)**: Mix any number of regional dialects and specialized jargon packs alongside base French & English dictionaries (14 built-in packs: `Ivorian French & Nouchi`, `Dioula & Baoulé Everyday Loanwords`, `African Urban Dialects`, `West African Culinary & Street Food`, `Daily Conversation`, `Abréviations & SMS (FR)`, `English Abbreviations & Slang`, `Gaming & Streaming Slang`, `Computer Science & Tech`, `Medicine & Healthcare`, `Biology & Life Sciences`, `Finance, Banking & Mobile Money`, `Law & Administration`, `Engineering & Architecture`), plus custom `.json` file import and starter template export for community submissions.
- **Consumer-Friendly Daylight Settings Redesign**: Clean off-white `#F5F6FA` canvas with pure white `#FFFFFF` grouped cards, emerald switches, everyday non-technical wording, and clickable **Author Rights & Licenses** articles opening a dedicated license & privacy reader.
- **Website-First Release & Compliance Prep**: Added `NOTICE`, offline `PRIVACY.md`, Adaptive Launcher Icon (`ic_launcher` + `ic_launcher_round`), R8/ProGuard JNI keep rules, and [`docs/STORE_DISTRIBUTION.md`](docs/STORE_DISTRIBUTION.md).

### Changed
- **Simplified Settings Defaults**: Removed all technical/expert controls (autocomplete engine picker, standalone Ivorian toggle, custom ms delay sliders, and emoji grid column editor—fixed cleanly at 8 columns per row).

### Fixed
- **Emoji History Circle Clipping**: Fixed horizontal scroll viewport slicing off edges of the active `🕐` category circle by giving the category bar vertical/horizontal inner clearance (`44.dp`) and bounding active scale to `1.0f`.
- **Alpha ↔ Emoji Switch Smoothness**: Eliminated transition stutter by unifying height between Keyboard+Toolbar and Emoji panel inside one `AnimatedContent`, removing texture `scaleIn` re-rasterization, hoisting `emojiGridState`, adding `contentType` keys, and pre-warming emoji/kaomoji assets on startup background thread.

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
