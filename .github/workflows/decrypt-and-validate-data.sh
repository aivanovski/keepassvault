#!/bin/bash

if [ -n "$GPG_PASSPHRASE" ]; then
  gpg --passphrase $GPG_PASSPHRASE --pinentry-mode loopback -o ./keys/debug.keystore -d ./keys/debug.keystore.gpg
  gpg --passphrase $GPG_PASSPHRASE --pinentry-mode loopback -o ./keys/release.keystore -d ./keys/release.keystore.gpg
  gpg --passphrase $GPG_PASSPHRASE --pinentry-mode loopback -o ./keys/google-cloud-key.json -d ./keys/google-cloud-key.json.gpg
  gpg --passphrase $GPG_PASSPHRASE --pinentry-mode loopback -o ./ci.properties -d ./ci.properties.gpg

  echo "3a15a7065ed4a62a747af2e3477b0a3e1940a7bc2946df638b902bcb186998e7 *keys/debug.keystore" | shasum --algorithm 256 --check
  echo "36a3bb8b7bda141b414c2df7fac7dcd09a2775769c561e562d2b43f7b246bfa5 *keys/release.keystore" | shasum --algorithm 256 --check
  echo "eac44fec5848bb6718b23ad433e73650352611953ae7f98c8e502c0491154da5 *keys/google-cloud-key.json" | shasum --algorithm 256 --check
  echo "b009011968e80d5b99eaa181141e6c94178a538c3dac0e413c43d5f95f50f557 *ci.properties" | shasum --algorithm 256 --check
else
  echo "Unable to decrypt secret data"
  exit 1
fi