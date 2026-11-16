# th-kb — Usage Guide

**th-kb** is a Gboard-parity Android Input Method Editor (IME) with power navigation, multi-mode deletion, and context-aware features inspired by `wm-keyboard`.

---

## 1. Enabling & Selecting `th-kb`

1. Open **th-kb Settings** from your app launcher.
2. Tap **Enable in Input Method Settings** and toggle **th-kb Keyboard** on.
3. Tap any text field (such as the **"Test keyboard here"** box in Settings) and select **th-kb Keyboard** from the system IME switcher (`🌐` at the bottom-right of your navigation bar).

---

## 2. Layouts & Context-Aware Modes

### Keyboard Layouts
- **QWERTY (English US)** and **AZERTY (French)** can be selected in **th-kb Settings** or switched via Android's system language/subtype switcher.
- Every opening of the keyboard starts in the **alphanumeric** layer (`ABC`), except for numeric/phone fields which open directly in the `?123` numeric/symbols layer.

### Symbol Layers
- Tap **`?123`** (bottom-left pill) to switch to numbers and primary symbols.
- Tap **`=\<`** (row 3 left key in `?123`) to access secondary math, currency, and bracket symbols.
- Tap **`ABC`** to return to the letter layer.

### Automatic Context Adaptation
`th-kb` inspects the active text field (`EditorInfo.inputType` and `packageName`) and adapts automatically:
- **Standard Text (`NORMAL`):** Enables auto-capitalization on open, after `.` / `!` / `?` + Space, and after `Return`.
- **URL Fields (`URL`):** Suppresses auto-Shift on open and replaces `,` on Row 4 with `/`.
- **Email Fields (`EMAIL`):** Suppresses auto-Shift on open and replaces `,` on Row 4 with `@`.
- **Password Fields (`PASSWORD`):** Suppresses auto-Shift on open.
- **Number / Phone Fields (`NUMBER`):** Automatically opens in the `?123` numeric/symbol layer.
- **Terminal Apps (`TERMINAL`, e.g. Termux):** Displays a scrollable **Terminal Special Bar** (`CTRL`, `ESC`, `TAB`, `^`, `<`, `>`, `←`, `↓`, `↑`, `→`, `-`, `/`, `|`, `~`, `&`, `$`) above the keys:
  - Tap **`CTRL`** to arm the Control modifier, then tap any letter (e.g. `c`, `d`, `z`, `l`) to send `Ctrl+<key>`.

---

## 3. Shift & Auto-Capitalization

- **Single Tap on `Shift`:** Toggles `Shift Once` (the hollow Shift arrow fills solid; automatically returns to lowercase after typing one character).
- **Double Tap on `Shift` (within `320ms`):** Locks `Caps Lock` (solid Shift arrow with an underline bar). Tap `Shift` once to exit `Caps Lock`.
- **Auto-Shift Triggers (when enabled in Settings):**
  1. Opening the keyboard in a normal text field.
  2. Typing `.`, `!`, or `?` followed by `Space`.
  3. Pressing `Return` (`Enter`) to start a new line/paragraph.

---

## 4. Long-Press Popups

Hold any letter, number, symbol, `.`, or `,` for `450ms` (configurable in Settings) to open a floating variant popup:
- **Finger-Locked Selection:** The primary hint/variant appears directly in the column above your finger (`selectedIndex` under finger).
- **2D Drag Selection:** Slide your finger left, right, or up (for 2-row popups) to highlight any variant and release to commit.
- **Cancel Before Popup Opens:** Gliding your finger before the long-press delay expires cancels the popup so fast gestures are never interrupted.
- **Dot (`.`) & Comma (`,`):** Long-pressing `.` or `,` opens a 2-row quick-punctuation grid (`…`, `!`, `?`, `:`, `;`, `-`, `'`, `"`, `(`, `)`, `/`, `@`, `#`, `&`, `%`, `+`).

---

## 5. Spacebar 2D Cursor Glide

Slide your finger across the **Spacebar** (`EN • FR`) to move the text cursor in 2 dimensions:
- **Glide Left / Right:** Moves the cursor character-by-character horizontally (`setSelection` with `DPAD_LEFT` / `DPAD_RIGHT` fallback).
- **Glide Up / Down:** Moves the cursor line-by-line vertically (`DPAD_UP` / `DPAD_DOWN`).

---

## 6. Power Delete Gestures on `Backspace`

The **`Backspace`** key supports both **Left-Delete** (default) and **Right-Delete (Forward Delete)** modes, plus multi-granularity swipe gestures:

| Gesture on `Backspace` | Action |
| :--- | :--- |
| **Glide Down** | Toggles between **Left-Delete** (`⌫`) and **Right-Delete** (`⌦`, mirrored icon & highlighted keycap). |
| **Single Tap** | Deletes **1 character** in the active delete direction (or deletes the active text selection). |
| **Very Long Press** (`580ms` default) | Starts **Rapid Character Delete** (`35ms` repeat) in the active delete direction. |
| **Glide Left** | Enters **Word Delete Mode** — deletes whole words in the active delete direction as you slide or hold left. |
| **Glide Up (to the top)** | Deletes **1 full paragraph / line** at once in the active delete direction. |

All delete delays and gesture toggles can be customized in **th-kb Settings → Delete & Long-Press Gestures**.

---

## 7. Emojis, Kaomoji & Favorites

Tap the **Emoji** icon in the top toolbar (or on Row 4) to open the unified picker panel:
- **`Emojis` Tab:** 7-column dark-themed emoji picker (`androidx.emoji2`).
- **`Kaomoji (^_^)` Tab:** Categorized ASCII emoticons (`all`, `happy`, `love`, `shrug`, `sad`, `angry`) with a `★` button on each card to favorite/unfavorite.
- **`★ Favorites` Tab:** Displays your pinned favorite emojis and kaomojis alongside your 24 most recently used emojis (tap `☆` on any recent emoji to pin it to Favorites).
- **Vertical Resizing:** Drag the top handle bar up/down (or tap the expand/collapse chevron in the toolbar) to resize the panel between `236dp` and `460dp`.
- **Floating Backspace:** Tap or hold the bottom-right Backspace button to delete without leaving the emoji panel.

---

## 8. Multi-Item Clipboard History

Tap the **Clipboard** icon in the top toolbar to open the Clipboard History panel:
- Stores up to **25 recent text clips** automatically.
- **Tap a clip** to insert it at the cursor.
- **Pin (`📌`)** important clips so they stay at the top and survive **Clear unpinned**.
- **Delete (`🗑`)** individual clips or tap **Paste from system** to trigger Android's native context-menu paste.
- Vertical resizing is automatically enabled when more than 4 clips are stored.
