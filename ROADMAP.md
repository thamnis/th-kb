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

## `v1.0` — Public Release

- [ ] Daily-driver stability, touch latency tuning, and `v0.x` bug fixes
- [ ] Layout & settings backup/export (JSON)

## `v1.1` — Ergonomics & Themes

- [ ] **Numeric-only keyboard mode** (dedicated large 3×4 numpad for `TYPE_CLASS_NUMBER` / `TYPE_CLASS_PHONE` fields)
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

- [ ] **Word suggestions & completion** (top-bar suggestion candidates backed by `rust-core`)
- [ ] **Dictionaries (`en`, `fr`), Personal dictionary & Word lookup** (offline EN/FR lexicons + user-learned/custom words)
- [ ] **Glided typing** (swipe path sampling + Rust trie/beam-search decoder)
- [ ] **Voice typing** (`SpeechRecognizer` / offline ASR)
- [ ] **Rambler mode** (continuous long-form voice dictation & structured text cleanup)
- [ ] **Language level metrics** (user vocabulary richness, CEFR level estimation, and typing stats)
- [ ] **OCR text scanner** (camera/screenshot optical character recognition directly into the active text field)
- [ ] **Fancy text generator** (live Unicode font styles: serif, script, fraktur, monospace, circled, small caps)
- [ ] **Built-in Note taker** (quick scratchpad notes panel inside the keyboard)
- [x] **Built-in Animated Todo list** (checkable task list panel inside keyboard with strike-through sweep, progress bar, paste-into-text, and smooth item animations)
- [ ] **Currency converter** (inline currency conversion calculator in the keyboard utility bar)
