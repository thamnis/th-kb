# th-kb Desktop Companion (`v1.3`)

Lightweight local Wi-Fi companion for **Remote KB & Mouse** in `th-kb`.

## Run on your computer

```bash
cargo run --manifest-path rust-companion/Cargo.toml --release
```

On Linux, install `xdotool` (X11) or `ydotool` (Wayland) so mouse movements, clicks, scrolling, and keystrokes are injected into the active desktop session. Pass `--dry-run` to inspect incoming packets without injecting OS input.

## Ports
- `55677/udp`: Remote touchpad movement, clicks, scrolling, key events, and text packets
- `55678/udp`: Local Wi-Fi auto-discovery (`THKB_DISCOVER`)
