# keepass-rs Android wrapper

This module owns Android integration for the root `keepass-rs` Rust crate.

## Build

Debug Rust binaries:

```bash
./gradlew :keepass-rs-android:buildRustJni -PrustProfile=debug
```

Release Rust binaries:

```bash
./gradlew :keepass-rs-android:buildRustJni
```

The wrapper also provides a direct script:

```bash
./keepass-rs-android/build-native-libraries.sh
./keepass-rs-android/build-native-libraries.sh --debug
```

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