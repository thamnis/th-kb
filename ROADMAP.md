# Roadmap

> Development follows a `v0.x` private POC and stabilization phase before the `v1.0` public release. All `v0.x` pre-release git tags must be tagged as `alpha` (e.g. `v0.1.0-alpha.1`).

## `v0.1` — Gboard-Parity POC (Internal)

- [x] `InputMethodService` + `ComposeView` lifecycle bridge (`LifecycleOwner`, `SavedStateRegistryOwner`)
- [x] Raw `pointerInput` (`awaitEachGesture`) touch grid for 0ms key-down & multi-touch chords
- [x] Pixel 7a `WindowInsets` & `res/xml/method.xml` subtypes (native bottom-left dismiss & bottom-right IME switcher)
- [x] Exact Gboard 4-row + top toolbar layout (`44dp` toolbar + `4 × 54dp` key rows)
- [x] Core keys: 3-state Shift, Delete (left tap/hold), Dynamic Return (`EditorInfo.imeOptions`), `?123` / `=\<` symbol layers
- [x] Multi-character long-press popups (accents & variants)
- [x] Spacebar glide-to-move-cursor (`setSelection` with `DPAD_LEFT/RIGHT` fallback)
- [x] Top toolbar: Paste (`performContextMenuAction`), Emoji/ASCII toggle, Settings shortcut
- [x] Emoji picker (`androidx.emoji2`) + static ASCII emoticons (`kaomoji`) panel
- [x] Minimal Settings screen (layout selection, haptic feedback, key-press sound)

## `v0.2` — Pre-Release Stabilization & Context

- [x] `InputConnection` edge-case fixes across Chrome, WhatsApp/Messages, Termux, and password/OTP fields
- [x] **Delete to the right** (glide-down on `Backspace` to toggle right-delete mode + word/paragraph/rapid delete gestures)
- [x] **Context-aware typing** (`EditorInfo.inputType` + `packageName` auto-switching for coding/terminals, messaging, numeric/symbols, URL, email, password)
- [x] Multi-item clipboard history (pin, delete, clear unpinned, system paste)
- [x] **Emoji & Kaomoji favorites** (pinned/starred emojis and kaomojis tab + high-DPI native Compose emoji grid with configurable column size)

## `v0.3` — Animated Todo List & Comprehensive Emojis

- [x] **Built-in Animated Todo List Panel** with task completion sweep, progress bar, paste into active text, and smooth animations
- [x] **Gboard-Style Bottom Emoji & Kaomoji Navigation** (`ABC` | `☺` | `★` | `:-)` | `⌫`) with animated indicator pills and sliding transitions
- [x] **Comprehensive Emojis Library** (complete Flags, comprehensive Symbols, Travel & Places)
- [x] **Two-Way Synchronized Category Navigation** with real-time scrolling synchronization
- [x] **Gliding Bubble Passing Transitions** in accent popup and key preview bubbles
- [x] **Blink-Free Instant Layer Switching**

## `v0.4` — Dedicated Numpad, Undo/Redo & Timestamped Backup

- [x] **Dedicated Numeric-Only Layout (Numpad)** with 3x4 phone layout, math operators, and long-press scientific variants
- [x] **Glide-Up Mode Toggle for Numeric Numpad** on `?123` / `ABC` with corner indicator hints (`123` / `?#`)
- [x] **Compact Undo & Redo Toolbar Widget** (`↺ | ↻`) with vertical bar divider and unlabelled sleek dimensions
- [x] **Timestamped JSON Backup & Restore** (`thkb_backup_yyyyMMdd_HHmmss.json`) for settings, clipboard history, favorites, and to-do lists

## `v0.5` — Text Suggestions, Dictionaries (EN, FR, Ivorian/Nouchi) & Autocomplete

- [ ] **High-Performance Rust Trie / FST Engine** in `rust-core` with sub-microsecond prefix lookups and Damerau-Levenshtein typo tolerance
- [ ] **Comprehensive & Tiny Dictionaries**:
  - English (`en`): compact frequency lexicon + complete inflected dictionary
  - French (`fr`): Lexique3-derived frequency lexicon + full conjugations
  - Ivorian French & Nouchi (`fr_CI` / `nouchi`): popular street vocabulary, expressions, and local phrasing
- [ ] **Optimal Context Window (Trigram / 64-char context)**: Contextual next-word prediction based on preceding tokens
- [ ] **3-Candidate Suggestion Strip**: Seamless top suggestion bar (`[literal] | [predicted/completed] | [next-word]`)
- [ ] **Composing Text Flow**: Native `InputConnection.setComposingText()` underlining and single-tap word replacement

## `v1.0` — Public Release

- [ ] Daily-driver stability, touch latency tuning, and `v0.x` bug fixes
- [x] Layout & settings backup/export (JSON) *(shipped in v0.4)*

## `v1.1` — Ergonomics & Themes

- [x] **Numeric-only keyboard mode** (dedicated large 3×4 numpad) *(shipped in v0.4)*
- [ ] **KB separation** (split mode)
- [ ] **One-hand mode** (left/right docked)
- [ ] **Floating KB** (`onComputeInsets` + `touchableInsets`)
- [ ] **Themes** (custom palettes, Material You dynamic colors)

## `v1.2` — Power Navigation & Rich Media

- [ ] Shortcuts to first/last characters (Home/End)
- [ ] Move cursor/selection by letter, word, phrase, and paragraph
- [ ] **Google Tasks synchronization** (link built-in Todo list panel to Google Tasks via OAuth 2.0 with offline-first bi-directional sync)
- [ ] **Media control & Music lyrics** (system media playback controls + live/searchable song lyrics insertion)
- [ ] **Rich media copy-pasting** (clipboard image/GIF items committed via `InputConnectionCompat.commitContent()`)
- [ ] **GIFs & Stickers** (`InputConnectionCompat.commitContent()`)
- [ ] **Emoji search** (keyword search inside the Emoji & Kaomoji panel backed by `rust-core`)
- [ ] **Discord-like `:shortcode:` emoji writing** (inline `:emoji_name:` autocomplete & replacement)

## `v1.3` — Cross-Device File & Remote Control

- [ ] **File sending** via **LocalSend** and **KDE Connect** protocols
- [ ] **Remote KB & mouse** for computer (local Wi-Fi / Bluetooth HID with Rust desktop companion)

## `v2.0` — Intelligence & Productivity Suite

- [ ] **Glided typing** (swipe path sampling + Rust trie/beam-search decoder)
- [ ] **Voice typing** (`SpeechRecognizer` / offline ASR)
- [ ] **Rambler mode** (continuous long-form voice dictation & structured text cleanup)
- [ ] **Language level metrics** (user vocabulary richness, CEFR level estimation, and typing stats)
- [ ] **OCR text scanner** (camera/screenshot optical character recognition directly into the active text field)
- [ ] **Fancy text generator** (live Unicode font styles: serif, script, fraktur, monospace, circled, small caps)
- [ ] **Built-in Note taker** (quick scratchpad notes panel inside the keyboard)
- [x] **Built-in Animated Todo list** *(shipped in v0.3)*
- [ ] **Currency converter** (inline currency conversion calculator in the keyboard utility bar)

## `v3.0` — Simple Mode & Distraction-Free Workflows

- [ ] **Simple Mode (Distraction-Free Keyboard)**: Minimalist, zero-gadget keyboard mode stripping away toolbars, accessory strips, and complex menus for pure, ultra-fast typing
- [ ] **High-Contrast Minimalist Aesthetic**: Clean typography, maximized key hitboxes, and distraction-free layout options
- [ ] **Instant Toggle & Battery Saver**: One-tap toggle between Full Feature Mode and Simple Mode, with zero background service overhead and ultra-light memory footprint
