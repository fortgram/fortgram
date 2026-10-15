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
#   ./scripts/setup-host.sh build       # gradle build + print the APK signature
#
# Package org.fgram.app is signed by Gradle: local.properties `keystore.file`
# points to a keystore.properties file (kept outside the repo) with
# keystore.file / keystore.password / key.alias / key.password.
#
# Designed to be idempotent — safe to re-run.

set -euo pipefail

# ---------- Configuration ----------
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
JAVA_HOME_DEFAULT="/usr/lib/jvm/temurin-25-jdk-$(dpkg --print-architecture 2>/dev/null || echo amd64)"
GRADLE_TASK="${GRADLE_TASK:-assembleFortgramArm64Release}"
KEYSTORE_PROPERTIES="${KEYSTORE_PROPERTIES:-$HOME/keystore.properties}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APK_DIR="$REPO_DIR/app/build/outputs/apk/fortgramArm64/release"

# ---------- Logging helpers ----------
log () { echo -e "\033[1;36m[setup]\033[0m $*"; }
warn () { echo -e "\033[1;33m[warn]\033[0m $*"; }
err () { echo -e "\033[1;31m[error]\033[0m $*" >&2; exit 1; }

# ---------- Step 1: system dependencies ----------
install_system_deps () {
  log "Installing system dependencies via apt"
  sudo apt-get update -q
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y \
    git git-lfs \
    yasm nasm make pkg-config \
    curl wget unzip gpg
  # Java 25 (required since upstream 1818) is not in the Ubuntu LTS archives — use Eclipse Adoptium's repository.
  if [ ! -d "$JAVA_HOME_DEFAULT" ]; then
    log "Installing Temurin JDK 25 from packages.adoptium.net"
    sudo install -d -m 0755 /etc/apt/keyrings
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor --yes -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo "$VERSION_CODENAME") main" \
      | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null
    sudo apt-get update -q
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y temurin-25-jdk
  fi
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
  log "Creating local.properties (fill in telegram.api_id / telegram.api_hash)"
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

# ---------- Step 6: report the signed APK ----------
report_apk () {
  local apk apksigner
  apk="$(ls -t "$APK_DIR"/FortGram-*.apk 2>/dev/null | head -1 || true)"
  [ -n "$apk" ] || err "Built APK not found in $APK_DIR"
  apksigner="$(ls -d "$ANDROID_SDK_ROOT"/build-tools/*/apksigner | sort -V | tail -1)"
  log "APK (signed by Gradle): $apk"
  "$apksigner" verify --print-certs "$apk" | grep -i "SHA-256 digest" || true
  # --no-incremental: incremental installs broke on some OEMs (package removed instead of updated).
  log "Install with: adb install -r --no-incremental $apk"
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
    report_apk
    ;;
  all|"")
    install_system_deps
    install_android_sdk
    write_local_properties
    sync_repo
    gradle_build
    report_apk
    ;;
  *)
    err "Unknown mode: $mode (use: deps | sync | build | all)"
    ;;
esac

log "Done."
