#!/usr/bin/env bash
# Fails if the PR changes the project version in pom.xml (<version> of the project or the
# <revision> property). The version comes from the git tags, created only by the deploy workflow.
#
# Usage: check-version-unchanged.sh <base-ref>   (e.g. origin/develop, from the root of the lambda)
set -euo pipefail

BASE_REF=${1:?Usage: $0 <base-ref>}

read_version() {
  python3 -c '
import sys, xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
root = ET.fromstring(sys.stdin.read())
version = root.findtext("m:version", default="", namespaces=ns).strip()
revision = root.findtext("m:properties/m:revision", default="", namespaces=ns).strip()
print(f"version={version} revision={revision}")
'
}

if ! git cat-file -e "$BASE_REF:pom.xml" 2>/dev/null; then
  echo "pom.xml doesn't exist in $BASE_REF, nothing to compare."
  exit 0
fi

BASE=$(git show "$BASE_REF:pom.xml" | read_version)
HEAD=$(read_version < pom.xml)

if [[ $BASE != "$HEAD" ]]; then
  echo "::error file=pom.xml::The project version can't be changed in a PR ($BASE → $HEAD)." \
    "It comes from the git tags v*, created by the workflow \"Deploy to test\"."
  exit 1
fi
echo "Project version unchanged ($HEAD)."
