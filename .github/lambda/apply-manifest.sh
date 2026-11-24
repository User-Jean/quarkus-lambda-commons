#!/usr/bin/env bash
# Applies the manifest of an environment (deploy/base.yaml + deploy/environments/<env>.yaml) to its
# lambda: memory, timeout, description and environment variables.
#
# Usage: apply-manifest.sh <test|prod> [--dry-run]   (from the root of the lambda)
#   --dry-run  merges and validates only, prints the manifest with the secrets unresolved (no AWS)
#   DESCRIPTION (env var, optional) overrides spec.description, the workflows pass the zip name
#   PROJECT_DIR (env var, default the current directory) the root of the lambda, with deploy/
#
# Requires yq v4 (mikefarah), jq, check-jsonschema and, without --dry-run, the AWS CLI.
set -euo pipefail

usage() {
  echo "Usage: $0 <test|prod> [--dry-run]" >&2
  exit 2
}

[[ $# -ge 1 ]] || usage
ENV_NAME=$1
MODE=${2:-apply}
[[ $ENV_NAME == test || $ENV_NAME == prod ]] || usage
[[ $MODE == apply || $MODE == --dry-run ]] || usage

ROOT=${PROJECT_DIR:-$PWD}
BASE=$ROOT/deploy/base.yaml
ENVIRONMENT_FILE=$ROOT/deploy/environments/$ENV_NAME.yaml
# Next to this script: the lambda runs the one of the version of quarkus-lambda-platform it uses
SCHEMA=$(cd "$(dirname "$0")" && pwd)/lambda-function.schema.json
[[ -f $BASE ]] || { echo "Manifest not found: $BASE (run it from the root of the lambda)" >&2; exit 1; }
[[ -f $ENVIRONMENT_FILE ]] || { echo "Environment not found: $ENVIRONMENT_FILE" >&2; exit 1; }

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
chmod 700 "$TMP"
MANIFEST=$TMP/manifest.json

# 1. Deep merge base + environment (the environment wins) and replace ${ENV} in every string
ENV="$ENV_NAME" yq eval-all -o=json \
  '. as $item ireduce ({}; . * $item) | (.. | select(tag == "!!str")) |= sub("[$][{]ENV[}]", strenv(ENV))' \
  "$BASE" "$ENVIRONMENT_FILE" > "$MANIFEST"

if grep -q '\${' "$MANIFEST"; then
  echo "Unresolved placeholder in the manifest of $ENV_NAME:" >&2
  grep -o '\${[^}]*}' "$MANIFEST" | sort -u >&2
  exit 1
fi

# 2. Validate against the schema
check-jsonschema --schemafile "$SCHEMA" "$MANIFEST"

DUPLICATED=$(jq -r '(.spec.secrets // {}) as $secrets | (.spec.env // {}) | keys[] | select(. as $key | $secrets | has($key))' "$MANIFEST")
if [[ -n $DUPLICATED ]]; then
  echo "Keys defined in both env and secrets: $DUPLICATED" >&2
  exit 1
fi

if [[ $MODE == --dry-run ]]; then
  echo "Manifest of $ENV_NAME is valid:"
  jq . "$MANIFEST"
  exit 0
fi

# 3. Resolve the secrets from SSM, masked in the GitHub Actions log
VARIABLES=$TMP/variables.json
jq '.spec.env // {}' "$MANIFEST" > "$VARIABLES"
while IFS=$'\t' read -r key reference; do
  value=$(aws ssm get-parameter --name "${reference#ssm:}" --with-decryption \
    --query Parameter.Value --output text < /dev/null)
  if [[ -n ${GITHUB_ACTIONS:-} ]]; then
    while IFS= read -r line; do
      if [[ -n $line ]]; then
        echo "::add-mask::$line"
      fi
    done <<< "$value"
  fi
  printf '%s' "$value" > "$TMP/value"
  jq --arg key "$key" --rawfile value "$TMP/value" '.[$key] = $value' "$VARIABLES" > "$TMP/next.json"
  mv "$TMP/next.json" "$VARIABLES"
  echo "Resolved secret $key from ${reference#ssm:}"
done < <(jq -r '(.spec.secrets // {}) | to_entries[] | [.key, .value] | @tsv' "$MANIFEST")
rm -f "$TMP/value"

# 4. Update the configuration. The request goes in a file so the secrets are not in the
# command line, and the output is discarded because it includes the environment variables
FUNCTION_NAME="$(jq -r .metadata.name "$MANIFEST")-$ENV_NAME"
REQUEST=$TMP/request.json
jq -n \
  --arg name "$FUNCTION_NAME" \
  --arg description "${DESCRIPTION:-$(jq -r '.spec.description // ""' "$MANIFEST")}" \
  --slurpfile manifest "$MANIFEST" \
  --slurpfile variables "$VARIABLES" \
  '{
    FunctionName: $name,
    MemorySize: $manifest[0].spec.memorySize,
    Timeout: $manifest[0].spec.timeout,
    Description: $description,
    Environment: { Variables: $variables[0] }
  }' > "$REQUEST"

# A previous update still in progress would make update-function-configuration fail
aws lambda wait function-updated --function-name "$FUNCTION_NAME"
aws lambda update-function-configuration --cli-input-json "file://$REQUEST" > /dev/null
aws lambda wait function-updated --function-name "$FUNCTION_NAME"

echo "Configuration of $FUNCTION_NAME updated:" \
  "memory $(jq .MemorySize "$REQUEST") MB, timeout $(jq .Timeout "$REQUEST") s," \
  "variables $(jq -r '.Environment.Variables | keys | join(", ")' "$REQUEST")"
