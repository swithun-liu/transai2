#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD_OUTPUT_DIR="$ROOT/composeApp/build/dist/wasmJs/productionExecutable"
DIST_DIR="$ROOT/dist"
AI_PROXY_URL="${TRANSAI_WEB_AI_PROXY_URL:-/api/chat/completions}"

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

mkdir -p \
  "$ROOT/.gradle-home" \
  "$ROOT/.kotlin-home" \
  "$ROOT/.home" \
  "$ROOT/.npm-cache" \
  "$ROOT/.yarn-cache"

cd "$ROOT"

export GRADLE_USER_HOME="$ROOT/.gradle-home"
export HOME="$ROOT/.home"
export NPM_CONFIG_CACHE="$ROOT/.npm-cache"
export YARN_CACHE_FOLDER="$ROOT/.yarn-cache"

echo "🔨 Building WebAssembly distribution..."
./gradlew \
  -Pkotlin.user.home="$ROOT/.kotlin-home" \
  -Pkotlin.compiler.execution.strategy=in-process \
  --no-build-cache \
  :composeApp:wasmJsBrowserDistribution

if [[ ! -d "$BUILD_OUTPUT_DIR" ]]; then
  echo "❌ Build output not found: $BUILD_OUTPUT_DIR" >&2
  exit 1
fi

echo "📁 Syncing build output to dist/ ..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
cp -R "$BUILD_OUTPUT_DIR"/. "$DIST_DIR"/

echo "🧩 Writing runtime-config.js ..."
cat > "$DIST_DIR/runtime-config.js" <<EOF
window.TRANSAI_RUNTIME_CONFIG = Object.assign({}, window.TRANSAI_RUNTIME_CONFIG, {
  aiProxyEndpoint: "$(json_escape "$AI_PROXY_URL")"
});
EOF

echo "✅ Web dist is ready in $DIST_DIR"
echo "🌐 AI proxy endpoint: $AI_PROXY_URL"
