#!/usr/bin/env bash
# Deploys a zip already in S3 to the lambda of an environment, publishes a version and points the
# alias "live" to it. Run it after apply-manifest.sh, so the version takes the code and the
# configuration together, and a rollback is moving "live" to the previous version.
#
# Usage: deploy-code.sh <test|prod> <s3-bucket> <s3-key> [version-description]
#   version-description is shown next to the version in the console (max 256 characters)
#   KEEP_VERSIONS (env var, default 5) versions kept, besides the one of "live"
#   PROJECT_DIR (env var, default the current directory) the root of the lambda, with deploy/
set -euo pipefail

[[ $# -eq 3 || $# -eq 4 ]] || { echo "Usage: $0 <test|prod> <s3-bucket> <s3-key> [version-description]" >&2; exit 2; }
ENV_NAME=$1
BUCKET=$2
KEY=$3
VERSION_DESCRIPTION=${4:-}
[[ ${#VERSION_DESCRIPTION} -le 256 ]] || { echo "The version description has more than 256 characters" >&2; exit 2; }
[[ $ENV_NAME == test || $ENV_NAME == prod ]] || { echo "Unknown environment: $ENV_NAME" >&2; exit 2; }

ROOT=${PROJECT_DIR:-$PWD}
FUNCTION_NAME="$(yq '.metadata.name' "$ROOT/deploy/base.yaml")-$ENV_NAME"
ALIAS=live
KEEP_VERSIONS=${KEEP_VERSIONS:-5}

aws lambda wait function-updated --function-name "$FUNCTION_NAME"
aws lambda update-function-code --function-name "$FUNCTION_NAME" \
  --s3-bucket "$BUCKET" --s3-key "$KEY" > /dev/null
aws lambda wait function-updated --function-name "$FUNCTION_NAME"

VERSION=$(aws lambda publish-version --function-name "$FUNCTION_NAME" \
  --description "$VERSION_DESCRIPTION" --query Version --output text)
aws lambda update-alias --function-name "$FUNCTION_NAME" --name "$ALIAS" --function-version "$VERSION" > /dev/null
echo "$FUNCTION_NAME:$ALIAS -> version $VERSION ($KEY)"

# Delete old versions, keeping the last KEEP_VERSIONS and always the one of the alias
LIVE_VERSION=$(aws lambda get-alias --function-name "$FUNCTION_NAME" --name "$ALIAS" \
  --query FunctionVersion --output text)
aws lambda list-versions-by-function --function-name "$FUNCTION_NAME" \
  --query 'Versions[?Version!=`$LATEST`].Version' --output text \
  | tr '\t' '\n' | sort -n \
  | awk -v keep="$KEEP_VERSIONS" '{ versions[NR] = $0 } END { for (i = 1; i <= NR - keep; i++) print versions[i] }' \
  | while read -r old; do
      if [[ -n $old && $old != "$LIVE_VERSION" ]]; then
        aws lambda delete-function --function-name "$FUNCTION_NAME" --qualifier "$old"
        echo "Deleted version $old"
      fi
    done
