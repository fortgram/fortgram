#!/usr/bin/env bash
# MiniGram host setup script.
# Bootstraps a fresh Ubuntu/Debian host with all dependencies needed to build
# MiniGram (Telegram-X fork) for arm64-v8a + armeabi-v7a, sign the APK with
# a debug keystore, and produce an installable file.
#
# Usage:
#   ./scripts/setup-host.sh             # full install + build
#   ./scripts/setup-host.sh deps        # system + Android SDK only
#   ./scripts/setup-host.sh native      # native libs (vpx/opus/ffmpeg) only
#   ./scripts/setup-host.sh build       # gradle build + sign only
#
# Designed to be idempotent — safe to re-run.

set -euo pipefail

# ---------- Configuration ----------
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
NDK_VERSION_PRIMARY="23.2.8568313"
NDK_VERSION_LEGACY="21.4.7075529"
CMAKE_VERSION="3.22.1"
BUILD_TOOLS_VERSION="36.0.0"
PLATFORM_VERSION="36"

JAVA_HOME_DEFAULT="/usr/lib/jvm/java-21-openjdk-amd64"
KEYSTORE_PATH="${KEYSTORE_PATH:-$HOME/debug.keystore}"
KEYSTORE_PASS="${KEYSTORE_PASS:-android}"
KEY_ALIAS="${KEY_ALIAS:-androiddebugkey}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APK_OUT="${APK_OUT:-$HOME/MiniGram-signed.apk}"

# ---------- Logging helpers ----------
log () { echo -e "\033[1;36m[setup]\033[0m $*"; }
warn () { echo -e "\033[1;33m[warn]\033[0m $*"; }
err () { echo -e "\033[1;31m[error]\033[0m $*" >&2; exit 1; }

require () { command -v "$1" >/dev/null 2>&1 || err "missing required command: $1"; }

# ---------- Step 1: system dependencies ----------
install_system_deps () {
  log "Installing system dependencies via apt"
  sudo apt-get update -q
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-21-jdk-headless \
    git git-lfs \
    yasm make \
    curl unzip \
    pkg-config
  git lfs install --skip-repo
}

# ---------- Step 2: Android SDK + NDK ----------
install_android_sdk () {
  log "Installing Android SDK to $ANDROID_SDK_ROOT"

  if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    local tmp; tmp="$(mktemp -d)"
    log "Downloading cmdline-tools"
    curl -fsSL -o "$tmp/tools.zip" \
      "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    unzip -q "$tmp/tools.zip" -d "$tmp"
    mv "$tmp/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    rm -rf "$tmp"
  fi

  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
  require sdkmanager

  log "Accepting SDK licenses"
  yes | sdkmanager --licenses >/dev/null 2>&1 || true

  log "Installing SDK packages (NDK $NDK_VERSION_PRIMARY, CMake $CMAKE_VERSION, build-tools $BUILD_TOOLS_VERSION)"
  sdkmanager \
    "platform-tools" \
    "platforms;android-$PLATFORM_VERSION" \
    "build-tools;$BUILD_TOOLS_VERSION" \
    "ndk;$NDK_VERSION_PRIMARY" \
    "ndk;$NDK_VERSION_LEGACY" \
    "cmake;$CMAKE_VERSION"
}

# ---------- Step 3: local.properties ----------
write_local_properties () {
  local f="$REPO_DIR/local.properties"
  if [ -f "$f" ]; then
    log "local.properties exists — leaving as-is"
    return
  fi
  log "Creating local.properties (you must fill in telegram.api_id / telegram.api_hash)"
  cat > "$f" <<EOF
sdk.dir=$ANDROID_SDK_ROOT

app.id=org.example.minigram
app.name=MiniGram
app.download_url=https://telegram.org/dlx
app.disable_signing=true

# Obtain from https://core.telegram.org/api/obtaining_api_id
telegram.api_id=
telegram.api_hash=

tgx.extension=none
EOF
  warn "Fill in telegram.api_id and telegram.api_hash in $f before building"
}

# ---------- Step 4: LFS + submodules ----------
sync_repo () {
  log "Pulling Git LFS objects (tdlib prebuilts)"
  cd "$REPO_DIR"
  git submodule update --init --recursive
  (cd tdlib && git lfs install && git lfs pull)
}

# ---------- Step 5: native dependencies ----------
build_native_deps () {
  log "Building native dependencies (vpx, opus, ffmpeg)"
  cd "$REPO_DIR"
  export TERM="${TERM:-xterm-256color}"
  export ANDROID_SDK_ROOT
  scripts/build-vpx.sh
  scripts/private/patch-opus-impl.sh
  scripts/build-ffmpeg.sh
}

# ---------- Step 6: keystore ----------
ensure_keystore () {
  if [ -f "$KEYSTORE_PATH" ]; then
    log "Keystore exists at $KEYSTORE_PATH"
    return
  fi
  log "Generating debug keystore at $KEYSTORE_PATH"
  keytool -genkey -v -keystore "$KEYSTORE_PATH" \
    -storepass "$KEYSTORE_PASS" -alias "$KEY_ALIAS" -keypass "$KEYSTORE_PASS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
}

# ---------- Step 7: Gradle build ----------
gradle_build () {
  log "Running ./gradlew assembleLatestUniversalRelease"
  cd "$REPO_DIR"
  export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
  export TERM="${TERM:-xterm-256color}"
  [ -d "$JAVA_HOME" ] || err "JAVA_HOME not found: $JAVA_HOME"
  ./gradlew assembleLatestUniversalRelease --no-daemon
}

# ---------- Step 8: sign APK ----------
sign_apk () {
  local apk_in="$REPO_DIR/app/build/outputs/apk/latestUniversal/release/MiniGram-0.28.8.1787.apk"
  [ -f "$apk_in" ] || err "Built APK not found: $apk_in"

  log "Signing APK -> $APK_OUT"
  "$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION/apksigner" sign \
    --ks "$KEYSTORE_PATH" \
    --ks-pass "pass:$KEYSTORE_PASS" \
    --key-pass "pass:$KEYSTORE_PASS" \
    --ks-key-alias "$KEY_ALIAS" \
    --out "$APK_OUT" \
    "$apk_in"
  log "Signed APK: $APK_OUT ($(du -h "$APK_OUT" | cut -f1))"
}

# ---------- Entry point ----------
mode="${1:-all}"

case "$mode" in
  deps)
    install_system_deps
    install_android_sdk
    ;;
  native)
    write_local_properties
    sync_repo
    build_native_deps
    ;;
  build)
    ensure_keystore
    gradle_build
    sign_apk
    ;;
  all|"")
    install_system_deps
    install_android_sdk
    write_local_properties
    sync_repo
    build_native_deps
    ensure_keystore
    gradle_build
    sign_apk
    ;;
  *)
    err "Unknown mode: $mode (use: deps | native | build | all)"
    ;;
esac

log "Done."
