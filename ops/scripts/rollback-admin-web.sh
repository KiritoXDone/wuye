#!/usr/bin/env bash
set -euo pipefail

TARGET_ROOT="/srv/wuye/admin-web"
CURRENT_LINK="$TARGET_ROOT/current"
PREVIOUS_LINK="$TARGET_ROOT/previous"

if [ ! -L "$PREVIOUS_LINK" ]; then
  echo "No previous release found"
  exit 1
fi

PREVIOUS_TARGET="$(readlink "$PREVIOUS_LINK")"
CURRENT_TARGET="$(readlink "$CURRENT_LINK" || true)"

ln -sfn "$PREVIOUS_TARGET" "$CURRENT_LINK"
if [ -n "$CURRENT_TARGET" ]; then
  ln -sfn "$CURRENT_TARGET" "$PREVIOUS_LINK"
fi

echo "Rolled back admin web to: $PREVIOUS_TARGET"
