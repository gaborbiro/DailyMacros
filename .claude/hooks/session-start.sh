#!/bin/bash
set -euo pipefail

# Only run in remote (web) sessions
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Register committed git hooks so pre-push checks run in this session
git -C "${CLAUDE_PROJECT_DIR:-.}" config core.hooksPath .claude/hooks

# ── Android SDK setup ──────────────────────────────────────────────────────────────────────────────
ANDROID_SDK_ROOT="${HOME}/android-sdk"
CMDLINE_TOOLS_VERSION="13114758"  # latest stable cmdline-tools

if [ ! -d "${ANDROID_SDK_ROOT}/cmdline-tools/latest" ]; then
  echo "Installing Android command-line tools..."
  mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
  TMP_ZIP=$(mktemp /tmp/cmdline-tools-XXXXXX.zip)
  curl -fsSL \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
    -o "${TMP_ZIP}"
  unzip -q "${TMP_ZIP}" -d "${ANDROID_SDK_ROOT}/cmdline-tools"
  mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" \
     "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
  rm "${TMP_ZIP}"
fi

export ANDROID_HOME="${ANDROID_SDK_ROOT}"
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

echo "Installing Android SDK components..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

# Persist env vars for the Claude Code session
echo "export ANDROID_HOME=${ANDROID_SDK_ROOT}" >> "${CLAUDE_ENV_FILE}"
echo "export PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:\$PATH" >> "${CLAUDE_ENV_FILE}"

# Write local.properties so Gradle finds the SDK
echo "sdk.dir=${ANDROID_SDK_ROOT}" > "${CLAUDE_PROJECT_DIR}/local.properties"

echo "Android SDK setup complete."
