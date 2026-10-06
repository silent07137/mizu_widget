#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail
: "${ANDROID_HOME:?Android SDK is required}"
: "${GITHUB_PATH:?Run this script in GitHub Actions}"
# Hosted runners install command-line tools without adding them to PATH.
mizu_sdk_manager="$(find "$ANDROID_HOME/cmdline-tools" -type f -name sdkmanager | sort -V | tail -n 1)"
if [ -z "$mizu_sdk_manager" ] || [ ! -x "$mizu_sdk_manager" ]; then
  echo "Android SDK command-line tools were not found."
  exit 1
fi
dirname "$mizu_sdk_manager" >> "$GITHUB_PATH"
printf '%s\n' "$ANDROID_HOME/platform-tools" "$ANDROID_HOME/emulator" >> "$GITHUB_PATH"
