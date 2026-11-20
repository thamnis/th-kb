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

- [x] **High-Performance Native Rust SymSpell + Spatial Engine** in `rust-core` (`arm64-v8a`, `armeabi-v7a`, `x86_64`) with sorted prefix buckets and layout-aware Damerau-Levenshtein typo tolerance
- [x] **30,000-Word Conversational Dictionaries + Ivorian Overlay**:
  - English (`en`): 30,000 cleaned OpenSubtitles conversational words + common bigrams/trigrams
  - French (`fr`): 30,000 cleaned OpenSubtitles conversational words + common bigrams/trigrams
  - Ivorian French & Nouchi (`fr_CI` / `nouchi`): authentic street vocabulary, expressions, city names, and colloquial phrasing
- [x] **Optimal Context Window (Trigram / 96-char context)**: Contextual next-word prediction and flexible `{autocomplete or advanced_autocomplete}` toggle
- [x] **3-Candidate Suggestion Strip & Auto-Replacement**: Dynamic strip with quoted `"literal"` slot, centered `#1` recommendation, space/punctuation auto-replace, and one-tap **Cancel-on-Delete (`⌫`)**
- [x] **On-Device Personal Dictionary & Adaptive Learning**: Local word/bigram learning, long-press suggestion blacklist, and Settings word management UI

## `v0.6` — UI/UX Stabilization, Modular Dialect Store & Distribution

- [x] **Consumer-Friendly Daylight Settings Redesign**: Modern `#F5F6FA` canvas + `#FFFFFF` grouped cards with zero technical settings/jargon, fixed 8 emoji columns, fixed smooth touch delays, and clickable Author Rights & Licenses reader
- [x] **Modular Dialect & Jargon Pack Store (`DialectPackManager`)**: Support installing, mixing, importing, and authoring custom `.json` jargon packs alongside base French & English (14 built-in packs covering Regional Dialects, Abbreviations, Conversation, Gaming, Tech/CS, Medicine, Biology, Finance/Mobile Money, Law, and Engineering)
- [x] **Emoji Panel Polish & Zero-Lag Transitions**: Fixed clipped history clock pill circle inside scrollable category bar; eliminated height jitter and scaling jank on Alpha ↔ Emoji switching
- [x] **Author Rights, Third-Party Licenses & Website Distribution Prep**: Added `NOTICE`, offline `PRIVACY.md`, Adaptive Launcher Icon (`ic_launcher`), ProGuard R8 JNI rules, and website-first plan ([`docs/STORE_DISTRIBUTION.md`](docs/STORE_DISTRIBUTION.md))
- [ ] **Website Release & Online Community Dialect Hub**: Sign website release APK (`v1.0.0`), launch web directory for community-submitted JSON dialect packs, and optionally prepare F-Droid / Google Play listings later

## `v0.7` — Glided Typing, Voice Typing & UI/UX Polish

- [x] **FUTO-Inspired Glided Typing + Disappearing Comet Trail**: Real-time `175ms` decaying comet trail + native Rust inflection-anchor & Gaussian DP swipe decoder across EN, FR, all 14 Dialect Store packs, and personal dictionary
- [x] **Word, Sentence & Char-by-Char Gliding**: Continuous sentence gliding through `SPACE` without lifting your finger, plus toggleable **Char-by-Char Glide Mode** (`Glide: Char`) and dwell-to-commit
- [x] **Reliable Voice Typing**: One-tap toolbar `Mic` button + live waveform panel, `FR`/`EN` toggle, Main-Looper `SpeechRecognizer` with automatic fallback to Android's system voice recognizer (`ACTION_RECOGNIZE_SPEECH`), and lifecycle-safe permission handling
- [x] **Deterministic IME Undo / Redo Engine**: In-IME text snapshot delta stack (`undoStack` / `redoStack`) working across all Android and Jetpack Compose text fields
- [x] **Keyboard & Emoji Visual Refinements**: Soft animated key-glide glow, spring-animated spacebar cursor slider, rounded-rectangular alphanum switch button with single accurate `123` / `?#` glide-up indicator, and larger `28.5.sp` tightly-spaced emojis

## `v0.8` — Contraction Elisions, Loop-Glide Disambiguation, Emoji Search & Modular UI

- [x] **Proposition A — French Contractions & Dynamic Elision Synthesis**: Typing or gliding `"cest"`, `"jai"`, `"lecole"`, `"daccord"` resolves `"c'est"`, `"j'ai"`, `"l'école"`, `"d'accord"` natively in Rust & Kotlin
- [x] **Proposition B — Loop / Dwell Double-Letter Glide Detection**: Detects micro-loops/dwells on doubled keys (`ll`, `nn`, `ss`, `pp`, `tt`) in `rust-core`
- [x] **Proposition C — Rust-Backed Bilingual Emoji & Kaomoji Search**: Inline `🔍` search strip inside the Emoji & Kaomoji panel matching English, French, and Ivorian/Nouchi tags
- [x] **Proposition D — Modular UI & Accessible On-Device Voice Discovery + Rust PCM Audio Analyzer**: Split `KeyboardRoot.kt` into dedicated `ui/` modules and bound `VoiceInputManager` directly to accessible offline-capable `RecognitionService` components + native Rust 16kHz PCM RMS analysis

## `v1.0` — Public Release

- [x] **Public Beta (`v1.0.0-beta.1`)**: R8-minified release build, full Dialect Store word browser, Shift glide-up mode switch, hold-Emoji hidden tools, 2D Spacebar cursor dot, live Voice streaming, and Alphanumeric Emoji search
- [ ] Final beta feedback, touch latency tuning, and signed `v1.0.0` website release
- [x] Layout & settings backup/export (JSON) *(shipped in v0.4)*

### Backlog — After `v1.0`

- [ ] **Char-Glided Symbols & Numbers**: Support character-by-character gliding across symbol (`?123`, `=\<`) and number (`123`) layers
- [ ] **Glide in Toolbar for Undo / Redo**: Swipe/glide horizontally across the top toolbar to step smoothly through Undo (`←`) and Redo (`→`) history snapshots

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

- [ ] **Rambler mode** (continuous long-form voice dictation & structured text cleanup)
- [ ] **Language level metrics** (user vocabulary richness, CEFR level estimation, and typing stats)
- [ ] **OCR text scanner** (camera/screenshot optical character recognition directly into the active text field)
- [ ] **Fancy text generator** (live Unicode font styles: serif, script, fraktur, monospace, circled, small caps)
- [ ] **Built-in Note taker** (quick scratchpad notes panel inside the keyboard)
- [x] **Built-in Animated Todo list** *(shipped in v0.3)*
- [ ] **Currency converter** (inline currency conversion calculator in the keyboard utility bar)

### Backlog — After `v2.0`

- [ ] **Glide-Typed Emoji & Glide-Typed Kaomoji**: Glide across keys or cells to type/predict emojis and kaomojis
- [ ] **Language-Specific Punctuation Rules**: Automatic typography and spacing rules per language (e.g. French spacing before `?`, `!`, `;`, `:` vs. English tight punctuation)

## `v3.0` — Simple Mode & Distraction-Free Workflows

- [ ] **Simple Mode (Distraction-Free Keyboard)**: Minimalist, zero-gadget keyboard mode stripping away toolbars, accessory strips, and complex menus for pure, ultra-fast typing
- [ ] **High-Contrast Minimalist Aesthetic**: Clean typography, maximized key hitboxes, and distraction-free layout options
- [ ] **Instant Toggle & Battery Saver**: One-tap toggle between Full Feature Mode and Simple Mode, with zero background service overhead and ultra-light memory footprint
