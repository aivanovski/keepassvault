#!/usr/bin/env bash

set -euo pipefail

if ! command -v maestro >/dev/null 2>&1; then
  echo "Maestro is not installed or is not available in PATH." >&2
  exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

exec maestro test \
  "$@" \
  "$script_dir/entry" \
  "$script_dir/file-picker" \
  "$script_dir/group" \
  "$script_dir/new-database" \
  "$script_dir/unlock"
