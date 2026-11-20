#!/usr/bin/env bash
# Prints the next version from the highest git tag vX.Y.Z (by semver, 0.0.0 when there are none).
#
# Usage: .github/scripts/next-version.sh <patch|minor|major>
set -euo pipefail

BUMP=${1:?Usage: $0 <patch|minor|major>}

# grep fails when there are no tags, which is the 0.0.0 case
LATEST=$({ git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true; } | sed 's/^v//' \
  | sort -t. -k1,1n -k2,2n -k3,3n | tail -n 1)
IFS='.' read -r MAJOR MINOR PATCH <<< "${LATEST:-0.0.0}"

case $BUMP in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
  *) echo "Unknown bump: $BUMP" >&2; exit 2 ;;
esac

echo "$MAJOR.$MINOR.$PATCH"
