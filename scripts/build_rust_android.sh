#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NDK_DIR="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.1.12297006}"
TOOLCHAIN_BIN="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin"
JNI_LIBS_DIR="$ROOT_DIR/app/src/main/jniLibs"

if [[ ! -d "$TOOLCHAIN_BIN" ]]; then
  echo "Android NDK toolchain not found at: $TOOLCHAIN_BIN" >&2
  exit 1
fi

mkdir -p "$JNI_LIBS_DIR/arm64-v8a" "$JNI_LIBS_DIR/armeabi-v7a" "$JNI_LIBS_DIR/x86_64"

export PATH="$TOOLCHAIN_BIN:$PATH"

echo "Building arm64-v8a (aarch64-linux-android)..."
CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$TOOLCHAIN_BIN/aarch64-linux-android24-clang" \
  CC_aarch64_linux_android="$TOOLCHAIN_BIN/aarch64-linux-android24-clang" \
  cargo build --manifest-path "$ROOT_DIR/rust-core/Cargo.toml" --release --target aarch64-linux-android
"$TOOLCHAIN_BIN/llvm-strip" "$ROOT_DIR/rust-core/target/aarch64-linux-android/release/libth_kb_core.so" \
  -o "$JNI_LIBS_DIR/arm64-v8a/libth_kb_core.so"

echo "Building armeabi-v7a (armv7-linux-androideabi)..."
CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_LINKER="$TOOLCHAIN_BIN/armv7a-linux-androideabi24-clang" \
  CC_armv7_linux_androideabi="$TOOLCHAIN_BIN/armv7a-linux-androideabi24-clang" \
  cargo build --manifest-path "$ROOT_DIR/rust-core/Cargo.toml" --release --target armv7-linux-androideabi
"$TOOLCHAIN_BIN/llvm-strip" "$ROOT_DIR/rust-core/target/armv7-linux-androideabi/release/libth_kb_core.so" \
  -o "$JNI_LIBS_DIR/armeabi-v7a/libth_kb_core.so"

echo "Building x86_64 (x86_64-linux-android)..."
CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER="$TOOLCHAIN_BIN/x86_64-linux-android24-clang" \
  CC_x86_64_linux_android="$TOOLCHAIN_BIN/x86_64-linux-android24-clang" \
  cargo build --manifest-path "$ROOT_DIR/rust-core/Cargo.toml" --release --target x86_64-linux-android
"$TOOLCHAIN_BIN/llvm-strip" "$ROOT_DIR/rust-core/target/x86_64-linux-android/release/libth_kb_core.so" \
  -o "$JNI_LIBS_DIR/x86_64/libth_kb_core.so"

ls -lh "$JNI_LIBS_DIR"/*/libth_kb_core.so
