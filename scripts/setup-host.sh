#!/usr/bin/env bash
# FortGram host setup script (Linux / Ubuntu builder).
# Bootstraps a fresh host with everything needed to build FortGram (Telegram-X
# fork) for arm64-v8a and produce an installable APK.
#
# Since upstream 1813 the native dependencies (ffmpeg, libvpx, opus) are built
# by Gradle tasks — no separate build-*.sh scripts. SDK / NDK versions come from
# version.properties via the upstream scripts/setup-sdk.sh.
#
# Usage:
#   ./scripts/setup-host.sh             # full install + build
#   ./scripts/setup-host.sh deps        # system packages + Android SDK only
#   ./scripts/setup-host.sh sync        # local.properties template + LFS + submodules
#   ./scripts/setup-host.sh build       # gradle build (+ sign for the legacy line)
#
# Two package lines exist while children's phones migrate:
#   legacy  — app.id=org.example.fortgram, app.disable_signing=true, APK signed
#             afterwards with the fixed debug keystore (KEYSTORE_PATH).
#   release — app.id=org.fgram.app, signed by Gradle: local.properties
#             `keystore.file` points to a keystore.properties file (kept outside
#             the repo) with keystore.file / keystore.password / key.alias /
#             key.password.
# Select with LINE=legacy|release (default: release).
#
# Designed to be idempotent — safe to re-run.

set -euo pipefail

# ---------- Configuration ----------
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
JAVA_HOME_DEFAULT="/usr/lib/jvm/java-21-openjdk-amd64"
LINE="${LINE:-release}"
GRADLE_TASK="${GRADLE_TASK:-assembleLatestArm64Release}"
KEYSTORE_PROPERTIES="${KEYSTORE_PROPERTIES:-$HOME/keystore.properties}"   # release line

# legacy line only
KEYSTORE_PATH="${KEYSTORE_PATH:-$HOME/.android/debug.keystore}"
KEYSTORE_PASS="${KEYSTORE_PASS:-android}"
KEY_ALIAS="${KEY_ALIAS:-androiddebugkey}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APK_DIR="$REPO_DIR/app/build/outputs/apk/latestArm64/release"

# ---------- Logging helpers ----------
log () { echo -e "\033[1;36m[setup]\033[0m $*"; }
warn () { echo -e "\033[1;33m[warn]\033[0m $*"; }
err () { echo -e "\033[1;31m[error]\033[0m $*" >&2; exit 1; }

# ---------- Step 1: system dependencies ----------
install_system_deps () {
  log "Installing system dependencies via apt"
  sudo apt-get update -q
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-21-jdk-headless \
    git git-lfs \
    yasm nasm make pkg-config \
    curl wget unzip
  git lfs install --skip-repo
}

# ---------- Step 2: Android SDK + NDK (versions from version.properties) ----------
install_android_sdk () {
  log "Installing Android SDK to $ANDROID_SDK_ROOT (versions from version.properties)"
  cd "$REPO_DIR"
  export ANDROID_SDK_ROOT TERM="${TERM:-xterm-256color}"
  export PATH="$REPO_DIR/scripts:$PATH"
  scripts/setup-sdk.sh
}

# ---------- Step 3: local.properties ----------
write_local_properties () {
  local f="$REPO_DIR/local.properties"
  if [ -f "$f" ]; then
    log "local.properties exists — leaving as-is"
    return
  fi
  log "Creating local.properties for the '$LINE' line (fill in telegram.api_id / telegram.api_hash)"
  if [ "$LINE" = "legacy" ]; then
    cat > "$f" <<EOF
sdk.dir=$ANDROID_SDK_ROOT

app.id=org.example.fortgram
app.name=FortGram
app.download_url=https://fgram.org
app.disable_signing=true

# Obtain from https://core.telegram.org/api/obtaining_api_id
telegram.api_id=
telegram.api_hash=

tgx.extension=none
EOF
  else
    cat > "$f" <<EOF
sdk.dir=$ANDROID_SDK_ROOT

app.id=org.fgram.app
app.name=FortGram
app.download_url=https://fgram.org

# Release signing: path to a keystore.properties file kept OUTSIDE the repo with
# keystore.file / keystore.password / key.alias / key.password
keystore.file=$KEYSTORE_PROPERTIES

# Obtain from https://core.telegram.org/api/obtaining_api_id
telegram.api_id=
telegram.api_hash=

tgx.extension=none
EOF
  fi
  chmod 600 "$f"
  warn "Fill in telegram.api_id and telegram.api_hash in $f before building"
}

# ---------- Step 4: LFS + submodules ----------
sync_repo () {
  log "Syncing submodules and Git LFS objects (tdlib prebuilts)"
  cd "$REPO_DIR"
  git submodule sync --recursive
  git submodule update --init --recursive --depth=1
  (cd tdlib && git lfs install && git lfs pull)
}

# ---------- Step 5: Gradle build ----------
gradle_build () {
  log "Running ./gradlew $GRADLE_TASK"
  cd "$REPO_DIR"
  export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
  export TERM="${TERM:-xterm-256color}"
  export ANDROID_SDK_ROOT
  [ -d "$JAVA_HOME" ] || err "JAVA_HOME not found: $JAVA_HOME"
  ./gradlew --no-daemon "$GRADLE_TASK"
}

# ---------- Step 6: sign APK (legacy line only) ----------
sign_apk () {
  local apk_in
  apk_in="$(ls -t "$APK_DIR"/FortGram-*.apk 2>/dev/null | grep -v -- '-signed.apk' | head -1 || true)"
  [ -n "$apk_in" ] || err "Built APK not found in $APK_DIR"
  if [ "$LINE" != "legacy" ]; then
    log "Release line: APK is signed by Gradle -> $apk_in"
    return
  fi
  [ -f "$KEYSTORE_PATH" ] || err "Legacy keystore not found: $KEYSTORE_PATH (the fixed debug key devices trust)"
  local apk_out="${apk_in%.apk}-signed.apk"
  local apksigner
  apksigner="$(ls -d "$ANDROID_SDK_ROOT"/build-tools/*/apksigner | sort -V | tail -1)"
  log "Signing APK (legacy line, v4 signature off) -> $apk_out"
  # --v4-signing-enabled false: incremental installs broke on some OEMs (package removed instead of updated).
  "$apksigner" sign --v4-signing-enabled false \
    --ks "$KEYSTORE_PATH" --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEYSTORE_PASS" \
    --ks-key-alias "$KEY_ALIAS" --out "$apk_out" "$apk_in"
  "$apksigner" verify --print-certs "$apk_out" | grep -i "SHA-1" || true
  log "Install with: adb install -r --no-incremental $apk_out"
}

# ---------- Entry point ----------
mode="${1:-all}"

case "$mode" in
  deps)
    install_system_deps
    install_android_sdk
    ;;
  sync)
    write_local_properties
    sync_repo
    ;;
  build)
    gradle_build
    sign_apk
    ;;
  all|"")
    install_system_deps
    install_android_sdk
    write_local_properties
    sync_repo
    gradle_build
    sign_apk
    ;;
  *)
    err "Unknown mode: $mode (use: deps | sync | build | all)"
    ;;
esac

log "Done."
