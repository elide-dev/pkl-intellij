#!/bin/bash
#
# Upload the plugin distribution built by `./gradlew buildPlugin` to our custom plugin repository.
#
# Required environment:
#   ELIDE_PLUGINS_URL (or PLUGINS_URL)  base URL of the plugin repository
#   ELIDE_PLUGINS_KEY                   API key for the plugin repository
#
set -euo pipefail

for tool in xmllint jq curl unzip; do
    if ! command -v "$tool" &> /dev/null; then
        echo "Error: $tool is required but not installed"
        exit 1
    fi
done

PLUGINS_URL="${ELIDE_PLUGINS_URL:-${PLUGINS_URL}}"

# the plugin version, since-build and until-build are patched into plugin.xml at build time, so the distribution
# archive is the only reliable source of metadata; the plugin.xml lives inside the plugin jar nested in the archive
shopt -s nullglob
DIST_FILES=(build/distributions/pkl-intellij-*.zip)
shopt -u nullglob

if [[ ${#DIST_FILES[@]} -eq 0 ]]; then
    echo "Error: no plugin archive found in build/distributions, run './gradlew buildPlugin' first"
    exit 1
fi
if [[ ${#DIST_FILES[@]} -gt 1 ]]; then
    echo "Error: multiple plugin archives found in build/distributions:"
    printf '  %s\n' "${DIST_FILES[@]}"
    echo "Remove the stale ones and retry"
    exit 1
fi

PLUGIN_FILE="${DIST_FILES[0]}"

# extract the patched plugin.xml, ignoring the searchable options jar
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

PLUGIN_JAR_ENTRY="$(unzip -Z1 "$PLUGIN_FILE" 'pkl-intellij/lib/pkl-intellij-*.jar' \
    | grep -v -- '-searchableOptions\.jar$' \
    | head -n 1)"

if [[ -z "$PLUGIN_JAR_ENTRY" ]]; then
    echo "Error: could not locate the plugin jar inside $PLUGIN_FILE"
    exit 1
fi

XML_FILE="$WORK_DIR/plugin.xml"
unzip -p "$PLUGIN_FILE" "$PLUGIN_JAR_ENTRY" > "$WORK_DIR/plugin.jar"
unzip -p "$WORK_DIR/plugin.jar" META-INF/plugin.xml > "$XML_FILE"

read_xpath() {
    xmllint --xpath "string(//idea-plugin/$1)" "$XML_FILE" 2>/dev/null
}

PLUGIN_ID=$(read_xpath "id")
PLUGIN_NAME=$(read_xpath "name")
PLUGIN_DESCRIPTION=$(read_xpath "description")
PLUGIN_VERSION=$(read_xpath "version")
PLUGIN_VENDOR=$(read_xpath "vendor")
PLUGIN_VENDOR_URL=$(read_xpath "vendor/@url")
SINCE_BUILD=$(read_xpath "idea-version/@since-build")
UNTIL_BUILD=$(read_xpath "idea-version/@until-build")

if [[ -z "$PLUGIN_ID" || -z "$PLUGIN_VERSION" ]]; then
    echo "Error: plugin id or version missing from $PLUGIN_JAR_ENTRY!META-INF/plugin.xml"
    exit 1
fi

echo "Uploading plugin:"
echo "Archive=$PLUGIN_FILE"
echo "ID=$PLUGIN_ID"
echo "Name=$PLUGIN_NAME"
echo "Version=$PLUGIN_VERSION"
echo "Vendor=${PLUGIN_VENDOR:-n/a}"
echo "Vendor URL=${PLUGIN_VENDOR_URL:-n/a}"
echo "Since build=${SINCE_BUILD:-n/a}"
echo "Until build=${UNTIL_BUILD:-n/a}"

url_encode() {
    jq -rn --arg str "$1" '$str | @uri'
}

ENCODED_ID=$(url_encode "$PLUGIN_ID")
ENCODED_VERSION=$(url_encode "$PLUGIN_VERSION")

echo "Getting presigned upload URL..."
UPLOAD_URL=$(curl --fail -s \
  -H "x-api-key: ${ELIDE_PLUGINS_KEY}" \
  "${PLUGINS_URL}/intellij/files/presign?id=${ENCODED_ID}&version=${ENCODED_VERSION}")

echo "Uploading plugin archive..."
curl --fail -# \
  -X PUT \
  -H "Content-Type: application/octet-stream" \
  --data-binary "@$PLUGIN_FILE" \
  "$UPLOAD_URL"
echo "Archive uploaded"

echo "Updating plugin metadata..."
METADATA_JSON="$(jq -n \
  --arg id "$PLUGIN_ID" \
  --arg name "$PLUGIN_NAME" \
  --arg description "$PLUGIN_DESCRIPTION" \
  --arg version "$PLUGIN_VERSION" \
  --arg vendor "$PLUGIN_VENDOR" \
  --arg vendorUrl "$PLUGIN_VENDOR_URL" \
  --arg sinceBuild "$SINCE_BUILD" \
  --arg untilBuild "$UNTIL_BUILD" \
  '{
    pluginId: $id,
    name: $name,
    description: $description,
    version: $version
  } +
  (if $vendor != "" then {vendorName: $vendor} else {} end) +
  (if $vendorUrl != "" then {vendorUrl: $vendorUrl} else {} end) +
  (if $sinceBuild != "" then {sinceBuild: $sinceBuild} else {} end) +
  (if $untilBuild != "" then {untilBuild: $untilBuild} else {} end)'
)"

curl --fail -s \
  -H "x-api-key: ${ELIDE_PLUGINS_KEY}" \
  -H "Content-Type: application/json" \
  "${PLUGINS_URL}/intellij/plugins?id=${ENCODED_ID}" \
  -d "$METADATA_JSON"
echo "Plugin deployed"
