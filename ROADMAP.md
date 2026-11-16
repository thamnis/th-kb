# Roadmap

> Development follows a `v0.x` private POC and stabilization phase before the `v1.0` public release.

## `v0.1` — Gboard-Parity POC (Internal)

- [x] `InputMethodService` + `ComposeView` lifecycle bridge (`LifecycleOwner`, `SavedStateRegistryOwner`)
- [x] Raw `pointerInput` (`awaitEachGesture`) touch grid for 0ms key-down & multi-touch chords
- [ ] Pixel 7a `WindowInsets` & `res/xml/method.xml` subtypes (native bottom-left dismiss & bottom-right IME switcher) — *subtypes configured; awaiting on-device Pixel 7a verification*
- [x] Exact Gboard 4-row + top toolbar layout (`44dp` toolbar + `4 × 54dp` key rows)
- [x] Core keys: 3-state Shift, Delete (left tap/hold), Dynamic Return (`EditorInfo.imeOptions`), `?123` / `=\<` symbol layers
- [x] Multi-character long-press popups (accents & variants)
- [x] Spacebar glide-to-move-cursor (`setSelection` with `DPAD_LEFT/RIGHT` fallback)
- [x] Top toolbar: Paste (`performContextMenuAction`), Emoji/ASCII toggle, Settings shortcut
- [x] Emoji picker (`androidx.emoji2`) + static ASCII emoticons (`kaomoji`) panel
- [x] Minimal Settings screen (layout selection, haptic feedback, key-press sound)

## `v0.2` — Pre-Release Stabilization & Context

- [ ] `InputConnection` edge-case fixes across Chrome, WhatsApp/Messages, Termux, and password/OTP fields
- [ ] **Delete to the right** (forward delete via `deleteSurroundingText(0, 1)`)
- [ ] **Context-aware typing** (`EditorInfo.inputType` + `packageName` auto-switching for coding/terminals, messaging, numeric-only, URL, password)
- [ ] Multi-item clipboard history

## `v1.0` — Public Release

- [ ] Daily-driver stability, touch latency tuning, and `v0.x` bug fixes
- [ ] Layout & settings backup/export (JSON)

## `v1.1` — Ergonomics & Themes

- [ ] **KB separation** (split mode)
- [ ] **One-hand mode** (left/right docked)
- [ ] **Floating KB** (`onComputeInsets` + `touchableInsets`)
- [ ] **Themes** (custom palettes, Material You dynamic colors)

## `v1.2` — Power Navigation & Rich Media

- [ ] Shortcuts to first/last characters (Home/End)
- [ ] Move cursor/selection by letter, word, phrase, and paragraph
- [ ] **GIFs & Stickers** (`InputConnectionCompat.commitContent()`)

## `v1.3` — Cross-Device File & Remote Control

- [ ] **File sending** via **LocalSend** and **KDE Connect** protocols
- [ ] **Remote KB & mouse** for computer (local Wi-Fi / Bluetooth HID with Rust desktop companion)

## `v2.0` — Intelligence

- [ ] **Glided typing** (swipe path sampling + Rust trie/beam-search decoder)
- [ ] **Hands-free typing** (`SpeechRecognizer` / offline ASR)
