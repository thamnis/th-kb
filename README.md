# th-kb — Custom Android Keyboard

A modular, privacy-first Android Input Method Editor (IME) built from scratch with **Kotlin (Jetpack Compose)** and **Rust**, starting with a Gboard-parity `v0.1` proof of concept on Google Pixel 7a and incorporating workflow/navigation inspirations from `wm-keyboard`.

## Tech Stack

- **Android / UI:** Kotlin, Jetpack Compose (`InputMethodService` + `ComposeView`), `androidx.emoji2`
- **Core Engine:** Rust (`cargo-ndk`, `uniffi-rs`)
- **Build & Config:** Gradle Kotlin DSL (`.kts`), Version Catalog (`gradle/libs.versions.toml`), Android XML (`AndroidManifest.xml`, `res/xml/method.xml`)
- **Layouts & Data:** JSON keymaps, multi-character accent maps, kaomoji catalogs

## Repository Structure

```text
.
├── app/                          # Android IME application module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml   # BIND_INPUT_METHOD service & VIBRATE permission
│       ├── assets/
│       │   ├── layouts/          # JSON keymaps (qwerty, symbols, accents)
│       │   └── kaomoji/          # Categorized ASCII emoticons
│       ├── kotlin/com/thamnis/thkb/
│       │   ├── ime/              # InputMethodService, lifecycle bridge, InputConnection
│       │   ├── ui/               # Compose key grid, toolbar, emoji/ASCII panels
│       │   ├── rust/             # Generated UniFFI bindings
│       │   └── settings/         # SettingsActivity & preferences
│       └── res/
│           └── xml/method.xml    # Subtypes (en_US, fr_FR) & IME switcher flag
├── rust-core/                    # Rust workspace (native .so via cargo-ndk + UniFFI)
│   ├── Cargo.toml
│   └── src/
│       ├── lib.rs                # UniFFI exports
│       ├── layout.rs             # Layout & accent lookup engine
│       └── search.rs             # Emoji / kaomoji search (and future decoders)
├── desktop-companion/            # (v1.3+) Rust receiver for remote KB/mouse & file transfer
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── CHANGELOG.md
├── CONTRIBUTING.md
├── LICENSE
├── README.md
└── ROADMAP.md
```

## Prerequisites & Build

- **JDK 17+** & **Android Studio / Android SDK** (API 35, NDK 27.1.12297006)
- **Rust toolchain** + Android targets + `cargo-ndk`:

```bash
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
```

Run Rust unit tests:

```bash
cargo test --manifest-path rust-core/Cargo.toml
```

Build the Rust native library and Android debug APK:

```bash
cargo ndk -t arm64-v8a -o app/src/main/jniLibs build --release --manifest-path rust-core/Cargo.toml
./gradlew assembleDebug
```

## Documentation, Roadmap & Changelog

- See [`docs/USAGE.md`](docs/USAGE.md) for the complete user guide (layers, Shift, long-press popups, 2D spacebar cursor glide, power delete gestures, emojis/favorites, clipboard history, and terminal mode).
- See [`ROADMAP.md`](ROADMAP.md) for the full milestone breakdown from `v0.1` (POC) through `v1.0` (public release) and beyond.
- See [`CHANGELOG.md`](CHANGELOG.md) for versioned release notes and `[Unreleased]` changes.
- See [`CONTRIBUTING.md`](CONTRIBUTING.md) for commit, branching, and maintenance conventions.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
