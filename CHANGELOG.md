# Changelog

All notable changes to **th-kb** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial repository structure for `th-kb` (`com.thamnis.thkb`) with Kotlin/Jetpack Compose Android IME module (`app/`) and Rust native core (`rust-core/`) using `uniffi-rs`.
- Project governance and documentation: `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, Apache-2.0 `LICENSE`, `.editorconfig`, `.gitignore`, and GitHub Actions CI workflow.
- Starter `InputMethodService` lifecycle bridge (`ThKbInputMethodService`), Compose keyboard stub (`KeyboardRoot`), and `SettingsActivity`.
- Starter JSON layout assets (`qwerty_en.json`, `symbols.json`, `accents.json`) and kaomoji catalog (`kaomoji.json`).
- Rust core layout/accent lookup (`layout.rs`) and kaomoji search (`search.rs`) modules with unit tests.
