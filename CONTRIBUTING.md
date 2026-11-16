# Contributing to th-kb

Thank you for contributing to **th-kb**! To keep the repository clean, predictable, and easy to maintain from `v0.1` onward, please follow these conventions.

## Commit Messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

- `feat(<scope>): ...` — new feature (e.g., `feat(ime): add 3-state shift key`)
- `fix(<scope>): ...` — bug fix (e.g., `fix(input): handle Termux backspace`)
- `docs(<scope>): ...` — documentation updates
- `refactor(<scope>): ...` — code restructuring without behavior change
- `test(<scope>): ...` — adding or updating tests
- `chore(<scope>): ...` — build, CI, dependencies, or tooling

Common scopes: `ime`, `ui`, `rust`, `layouts`, `settings`, `ci`, `docs`.

## Changelog Policy

Every user-facing change, bug fix, or notable architectural change should include an entry under `## [Unreleased]` in `CHANGELOG.md` using the Keep a Changelog categories (`Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`).

## Local Verification Before Pushing

Run Rust tests and Android checks before opening a PR or merging to `main`:

```bash
cargo test --manifest-path rust-core/Cargo.toml
cargo clippy --manifest-path rust-core/Cargo.toml --all-targets -- -D warnings
./gradlew testDebugUnitTest lintDebug
```
