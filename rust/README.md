# Rust Android module

This directory contains the Rust JNI library used by the Android app.

## Layout

- `passnotes-rust/`: Rust crate that builds `libpassnotes_rust.so`
- `app/src/main/kotlin/com/ivanovsky/passnotes/domain/rust/RustBridge.kt`: Kotlin bridge used to call native methods

## Prerequisites

1. Install the Android NDK with the SDK manager.
2. Install `cargo-ndk`:

   ```bash
   cargo install cargo-ndk
   ```

3. Install Rust Android targets:

   ```bash
   rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
   ```

If Android Studio does not inherit your shell `PATH`, Gradle can resolve Cargo from
`~/.cargo/bin` automatically. For non-standard installs, add one of these entries to
`local.properties`:

```properties
cargo.path=/absolute/path/to/cargo
# or
cargo.home=/absolute/path/to/.cargo
```

## Build

The Android build now packages the Rust library automatically for JNI merge tasks.

Debug build:

```bash
./gradlew assembleFdroidDebug
```

Release build:

```bash
./gradlew assembleGplayRelease
```

You can force the Rust profile explicitly if needed:

```bash
./gradlew assembleFdroidDebug -PrustProfile=debug
./gradlew assembleGplayRelease -PrustProfile=release
```

You can also invoke the Rust build directly:

```bash
./gradlew buildRustJni
```

## Kotlin usage

Example call from Android code:

```kotlin
val answer = RustBridge.nativeAdd(20, 22)
```

Replace `nativeAdd()` with your own JNI methods as the Rust library grows.
