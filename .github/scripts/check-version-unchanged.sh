#!/usr/bin/env bash
# Fails if a pom.xml of the repository writes a version by hand. The version comes only from the git tags v*
# (maven-git-versioning-extension, .mvn/), created by the workflow "Release":
# - <version> of each project and of its parent inside the repository is ${revision} (or inherited)
# - the properties revision and quarkus-lambda-commons.version keep their placeholder 0.0.0-SNAPSHOT
#
# Usage: .github/scripts/check-version-unchanged.sh
set -euo pipefail

# Not the pom of archetype-resources: it is the project an archetype generates, with the version of the library as a
# token that archetype-post-generate.groovy replaces
git ls-files '*pom.xml' ':!:**/archetype-resources/**' | python3 -I -c '
import sys, xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
PLACEHOLDER = "0.0.0-SNAPSHOT"
errors = []
for path in sys.stdin.read().split():
    root = ET.parse(path).getroot()
    version = root.findtext("m:version", default="", namespaces=ns).strip()
    if version not in ("", "${revision}"):
        errors.append(f"{path}: <version>{version}</version>, must be ${{revision}}")
    if root.findtext("m:parent/m:groupId", default="", namespaces=ns).strip() == "com.xdyan":
        parent = root.findtext("m:parent/m:version", default="", namespaces=ns).strip()
        if parent != "${revision}":
            errors.append(f"{path}: parent <version>{parent}</version>, must be ${{revision}}")
    for name in ("revision", "quarkus-lambda-commons.version"):
        value = root.findtext(f"m:properties/m:{name}", default=None, namespaces=ns)
        if value is not None and value.strip() != PLACEHOLDER:
            errors.append(f"{path}: <{name}>{value.strip()}</{name}>, must be {PLACEHOLDER}")
for error in errors:
    print(f"::error::{error}. The version comes from the git tags v*, created by the workflow \"Release\".")
if errors:
    sys.exit(1)
print("No version written by hand.")
'
