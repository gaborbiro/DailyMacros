#!/bin/bash
set -euo pipefail

echo "session-start hook: running (CLAUDE_CODE_REMOTE=${CLAUDE_CODE_REMOTE:-<unset>})"

# Only run in remote (web) sessions
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  echo "session-start hook: not a remote session, skipping setup."
  exit 0
fi

# Register committed git hooks so pre-push checks run in this session
git -C "${CLAUDE_PROJECT_DIR:-.}" config core.hooksPath .claude/hooks

# ── Gradle setup ───────────────────────────────────────────────────────────────────────────────────

# Let the wrapper provision the exact version pinned in
# gradle/wrapper/gradle-wrapper.properties (a hardcoded version here would go
# stale, and the wrapper only recognises distributions installed under its own
# URL-hashed directory anyway).
echo "Priming Gradle wrapper (version from gradle-wrapper.properties)..."
if (cd "${CLAUDE_PROJECT_DIR:-.}" && ./gradlew --version); then
  echo "Gradle wrapper ready."
else
  echo "WARNING: Could not provision the Gradle wrapper." \
       "Build commands will not work in this session." \
       "Create a CCR environment with network access to services.gradle.org / github.com to fix this."
fi

# ── Android SDK setup ──────────────────────────────────────────────────────────────────────────────

# Check well-known pre-installed locations before attempting a download
ANDROID_SDK_ROOT=""
for candidate in /opt/android-sdk /usr/local/lib/android/sdk "${HOME}/android-sdk"; do
  if [ -d "${candidate}/platforms" ]; then
    ANDROID_SDK_ROOT="${candidate}"
    echo "Found pre-installed Android SDK at ${ANDROID_SDK_ROOT}"
    break
  fi
done

if [ -z "${ANDROID_SDK_ROOT}" ]; then
  # No pre-installed SDK found — download cmdline-tools and install components
  ANDROID_SDK_ROOT="${HOME}/android-sdk"
  CMDLINE_TOOLS_VERSION="13114758"  # latest stable cmdline-tools
  echo "Installing Android command-line tools..."
  mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools"
  TMP_ZIP=$(mktemp /tmp/cmdline-tools-XXXXXX.zip)
  if curl -fsSL \
      "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" \
      -o "${TMP_ZIP}"; then
    unzip -q "${TMP_ZIP}" -d "${ANDROID_SDK_ROOT}/cmdline-tools"
    mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" \
       "${ANDROID_SDK_ROOT}/cmdline-tools/latest"
    rm "${TMP_ZIP}"

    export ANDROID_HOME="${ANDROID_SDK_ROOT}"
    export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

    echo "Installing Android SDK components..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    sdkmanager \
      "platform-tools" \
      "platforms;android-36" \
      "build-tools;36.0.0"
    echo "Android SDK setup complete."
  else
    rm -f "${TMP_ZIP}"
    echo "WARNING: Could not download Android SDK." \
         "Build commands will not work in this session." \
         "Create a CCR environment with network access to dl.google.com to fix this."
  fi
fi

if [ -n "${ANDROID_SDK_ROOT}" ] && [ -d "${ANDROID_SDK_ROOT}/platforms" ]; then
  export ANDROID_HOME="${ANDROID_SDK_ROOT}"
  export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

  # Persist env vars for the Claude Code session; CLAUDE_ENV_FILE is not
  # guaranteed to be set, and referencing it unguarded under `set -u` would
  # abort the whole hook here
  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    echo "export ANDROID_HOME=${ANDROID_SDK_ROOT}" >> "${CLAUDE_ENV_FILE}"
    echo "export PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:\$PATH" >> "${CLAUDE_ENV_FILE}"
  else
    echo "WARNING: CLAUDE_ENV_FILE not set; ANDROID_HOME/PATH not persisted for the session."
  fi

  # Write local.properties so Gradle finds the SDK
  echo "sdk.dir=${ANDROID_SDK_ROOT}" > "${CLAUDE_PROJECT_DIR}/local.properties"
fi

echo "session-start hook: done"
