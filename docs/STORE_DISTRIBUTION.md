# App Distribution & Community Dialect Store Plan

This document tracks **th-kb** (`com.thamnis.thkb`) distribution readiness, starting with direct downloads on your **personal website** and preparing for optional future **F-Droid** and **Google Play Store** listings.

---

## 1. Phase 1 (Primary): Personal Website Distribution

1. **Direct Signed APK Download + Checksums**:
   - Sign release builds with your private release keystore (`thkb-release.jks`, kept outside Git).
   - Enable R8 minification & resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`) verified with [`app/proguard-rules.pro`](../app/proguard-rules.pro).
   - Publish `th-kb-vX.Y.Z.apk` + `.sha256` checksum on your website alongside an agreement banner linking [`PRIVACY.md`](../PRIVACY.md) and [`NOTICE`](../NOTICE).

2. **Website Community Dialect & Jargon Store**:
   - Host modular `.json` Dialect & Jargon Packs (Regional slangs, Computer Science, Medicine, Abbreviations, Conversation) on your website so users can browse, download, and tap **Import Dialect Pack** directly inside **Settings → Dialect & Jargon Store**.
   - Provide a community submission form where contributors export a starter template from th-kb, customize words/phrases, and submit their JSON file for review before publishing to the website catalog.

---

## 2. Technical & Legal Compliance Baseline

| Requirement | Status | Details |
| :--- | :--- | :--- |
| **Application ID** | ✅ Ready | `com.thamnis.thkb` (`app/build.gradle.kts`) |
| **Target API Level** | ✅ Ready | `targetSdk = 35` |
| **64-bit Native Binaries** | ✅ Ready | `arm64-v8a` (`libth_kb_core.so`) + `armeabi-v7a` + `x86_64` compiled via NDK 27 |
| **Offline / Zero Permissions** | ✅ Ready | Only `android.permission.VIBRATE`; zero `INTERNET` or telemetry |
| **Adaptive Launcher Icon** | ✅ Ready | `ic_launcher` & `ic_launcher_round` (`res/mipmap-anydpi-v26/`) |
| **ProGuard / R8 Keep Rules** | ✅ Ready | Protects `NativeSuggestionBridge`, JNI, and JNA/UniFFI |
| **Clickable Author Rights & Licenses** | ✅ Ready | Interactive **Author Rights & Licenses** reader in Settings + [`NOTICE`](../NOTICE) |

---

## 3. Phase 2 (Optional Later): F-Droid & Google Play Store

Once the personal website release is battle-tested and stable:
- **F-Droid**: Keep prebuilt binaries out of Git (`jniLibs/` is `.gitignore`d), add Fastlane store metadata (`fastlane/metadata/android/en-US/`), and submit a `fdroiddata` build recipe calling `./scripts/build_rust_android.sh` cleanly from source.
- **Google Play Store**: Generate signed `.aab` (`./gradlew bundleRelease`), link your website's hosted Privacy Policy URL, complete Play Data Safety ("No data collected / shared"), upload Feature Graphic (`1024×500`) + phone screenshots, and complete closed testing.
