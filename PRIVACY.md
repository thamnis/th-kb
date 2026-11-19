# Privacy Policy — th-kb

**Effective Date:** 2026-11-17  
**Author / Maintainer:** Thamnis (`com.thamnis.thkb`)

## 100% Offline & Zero Telemetry

**th-kb** is an offline-first open-source Android keyboard (`InputMethodService`). We do not collect, transmit, sell, or share any user data.

### Permissions Used
- `android.permission.VIBRATE`: Used solely for optional tactile keypress haptic feedback.
- `android.permission.RECORD_AUDIO`: Requested only on demand if you tap **Voice Typing** (`Mic`) to dictate text; prefers Android's on-device speech recognizer (`API 31+`) and never listens in the background.
- **`INTERNET` permission is NOT requested**: The application has zero network access and cannot send keystrokes, clipboard items, passwords, or usage statistics outside your device.

### On-Device Local Data
All personalization and productivity data remains strictly in private local storage on your device (`SharedPreferences` / local JSON):
- **Settings & Preferences**: Layout selection, key-press delays, autocomplete mode, and suggestion toggles.
- **Personal Dictionary & Adaptive Learning**: Words, names, and word pairs learned locally by your device. Password (`TYPE_TEXT_VARIATION_PASSWORD` / `TYPE_NUMBER_VARIATION_PASSWORD`) and numeric/OTP fields are explicitly excluded from learning and suggestions.
- **Clipboard History & To-Do List**: Stored strictly locally until cleared or exported by you via manual JSON backup.

### Data Export & Deletion
- You can inspect, edit, or delete learned words in **Settings → Personal Dictionary**, or wipe everything instantly with **Clear Learned History**.
- Manual backups are written only when explicitly triggered via **Settings → Backup & Restore** to a user-chosen file location.
