#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "Usage: $0 <dist-dir> <release-name>"
  exit 1
fi

DIST_DIR="$1"
RELEASE_NAME="$2"
TARGET_ROOT="/srv/wuye/admin-web"
RELEASE_DIR="$TARGET_ROOT/releases/$RELEASE_NAME"
CURRENT_LINK="$TARGET_ROOT/current"
PREVIOUS_LINK="$TARGET_ROOT/previous"

mkdir -p "$TARGET_ROOT/releases"
rm -rf "$RELEASE_DIR"
cp -R "$DIST_DIR" "$RELEASE_DIR"

if [ -L "$CURRENT_LINK" ] || [ -d "$CURRENT_LINK" ]; then
  CURRENT_TARGET="$(readlink "$CURRENT_LINK" || true)"
  if [ -n "$CURRENT_TARGET" ]; then
    ln -sfn "$CURRENT_TARGET" "$PREVIOUS_LINK"
  fi
fi

ln -sfn "$RELEASE_DIR" "$CURRENT_LINK"
echo "Published admin web release: $RELEASE_NAME"
