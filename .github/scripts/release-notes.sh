#!/usr/bin/env bash
# Prints the notes of the release of a tag: its description and the complete changelog, one line per
# pull request merged since the previous tag ("- #26 - Title"). A commit pushed without a pull request
# is listed with its short sha. Needs the full history and tags (fetch-depth: 0) and gh with GH_TOKEN.
#
# Usage: .github/scripts/release-notes.sh <tag> [description]
set -euo pipefail

TAG=${1:?Usage: $0 <tag> [description]}
DESCRIPTION=${2:-}
REPO=${GITHUB_REPOSITORY:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}

# The highest tag vX.Y.Z below this one, by semver; none on the first release
PREVIOUS=$({ git tag -l 'v*' | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' || true; } \
  | sort -V \
  | awk -v tag="$TAG" '$0 == tag { print last; exit } { last = $0 }')
RANGE=${PREVIOUS:+$PREVIOUS..}$TAG

SEEN=" "
LINES=()
# Oldest first, without the merge commits of the branches (their pull request is found by its commits)
while read -r sha subject; do
  pulls=$(gh api "repos/$REPO/commits/$sha/pulls" \
    -q '.[] | select(.merged_at != null) | "\(.number)\t\(.title)"' 2>/dev/null || true)
  if [[ -z $pulls ]]; then
    LINES+=("- ${sha:0:7} - $subject")
    continue
  fi
  while IFS=$'\t' read -r number title; do
    if [[ $SEEN != *" $number "* ]]; then
      SEEN+="$number "
      LINES+=("- #$number - $title")
    fi
  done <<< "$pulls"
done < <(git log --reverse --no-merges --format='%H %s' "$RANGE")

if [[ -n $DESCRIPTION ]]; then
  printf '%s\n\n' "$DESCRIPTION"
fi
echo "## Complete changelog"
echo
if [[ ${#LINES[@]} -eq 0 ]]; then
  echo "No changes."
else
  printf '%s\n' "${LINES[@]}"
fi
if [[ -n $PREVIOUS ]]; then
  echo
  echo "**Compare**: https://github.com/$REPO/compare/$PREVIOUS...$TAG"
fi
