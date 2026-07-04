#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
readonly CRATE_DIR="$REPO_ROOT/keepass-rs"
readonly LOCAL_PROPERTIES_PATH="$REPO_ROOT/local.properties"
readonly OUTPUT_DIR="$SCRIPT_DIR/src/main/jniLibs"
readonly LIBRARY_NAME="libkeepass_rs.so"
readonly ANDROID_API_LEVEL="26"
readonly ABIS=("armeabi-v7a" "arm64-v8a" "x86_64")

build_mode="release"

usage() {
    cat <<'EOF'
Usage: ./keepass-rs-android/build-native-libraries.sh [--debug]

Builds Android Rust JNI libraries for:
- armeabi-v7a
- arm64-v8a
- x86_64

Options:
  --debug   Build debug binaries instead of release binaries
  --help    Show this help message
EOF
}

require_command() {
    local command_name="$1"

    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command is not installed: $command_name" >&2
        exit 1
    fi
}

decode_properties_path() {
    local path_value="$1"

    printf '%s' "$path_value" | sed 's#\\\\#\\#g; s#\\:#:#g; s#\\=#=#g; s#\\ # #g'
}

resolve_sdk_root() {
    local sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

    if [[ -n "$sdk_root" ]]; then
        printf '%s\n' "$sdk_root"
        return 0
    fi

    if [[ -f "$LOCAL_PROPERTIES_PATH" ]]; then
        local sdk_dir_line
        sdk_dir_line="$(grep '^sdk\.dir=' "$LOCAL_PROPERTIES_PATH" || true)"

        if [[ -n "$sdk_dir_line" ]]; then
            decode_properties_path "${sdk_dir_line#sdk.dir=}"
            return 0
        fi
    fi

    echo "Unable to resolve Android SDK path. Set ANDROID_SDK_ROOT or add sdk.dir to local.properties." >&2
    exit 1
}

resolve_ndk_home() {
    local sdk_root="$1"
    local ndk_home="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${NDK_HOME:-}}}"

    if [[ -n "$ndk_home" ]]; then
        printf '%s\n' "$ndk_home"
        return 0
    fi

    local ndk_root="$sdk_root/ndk"
    if [[ ! -d "$ndk_root" ]]; then
        echo "Android NDK was not found under $ndk_root." >&2
        exit 1
    fi

    local detected_ndk
    detected_ndk="$(
        find "$ndk_root" -mindepth 1 -maxdepth 1 -type d -exec basename {} \; |
            sort -t '.' -k1,1n -k2,2n -k3,3n -k4,4n |
            tail -n 1
    )"

    if [[ -z "$detected_ndk" ]]; then
        echo "Android NDK was not found under $ndk_root." >&2
        exit 1
    fi

    printf '%s\n' "$ndk_root/$detected_ndk"
}

remove_existing_libraries() {
    for abi in "${ABIS[@]}"; do
        rm -f -- "$OUTPUT_DIR/$abi/$LIBRARY_NAME"
    done
}

while (($# > 0)); do
    case "$1" in
        --debug)
            build_mode="debug"
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
    shift
done

require_command cargo

if ! cargo ndk --help >/dev/null 2>&1; then
    echo "cargo-ndk is not installed. Install it with: cargo install cargo-ndk" >&2
    exit 1
fi

if [[ ! -d "$CRATE_DIR" ]]; then
    echo "Rust crate directory was not found: $CRATE_DIR" >&2
    exit 1
fi

sdk_root="$(resolve_sdk_root)"
ndk_home="$(resolve_ndk_home "$sdk_root")"
if [[ ! -d "$sdk_root" ]]; then
    echo "Android SDK path does not exist: $sdk_root" >&2
    exit 1
fi

if [[ ! -d "$ndk_home" ]]; then
    echo "Android NDK path does not exist: $ndk_home" >&2
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

build_args=(ndk)
for abi in "${ABIS[@]}"; do
    build_args+=(-t "$abi")
done
build_args+=(-P "$ANDROID_API_LEVEL" -o "$OUTPUT_DIR" build)

if [[ "$build_mode" == "release" ]]; then
    build_args+=(--release)
fi

echo "Building Android Rust JNI libraries in $build_mode mode"
echo "Using Android SDK: $sdk_root"
echo "Using Android NDK: $ndk_home"
echo "Output directory: $OUTPUT_DIR"
echo "Build args: ${build_args[@]}"

remove_existing_libraries

(
    cd "$CRATE_DIR"
    export ANDROID_SDK_ROOT="$sdk_root"
    export ANDROID_NDK_HOME="$ndk_home"
    cargo "${build_args[@]}"
)

echo "Built libraries:"
for abi in "${ABIS[@]}"; do
    echo "- $OUTPUT_DIR/$abi/$LIBRARY_NAME"
done
