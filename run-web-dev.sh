#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
PORT="${TRANSAI_DEV_SERVER_PORT:-8080}"

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

stop_existing_dev_server() {
  if ! command -v lsof >/dev/null 2>&1; then
    return
  fi

  local pids
  pids="$(lsof -ti "tcp:${PORT}" -sTCP:LISTEN 2>/dev/null || true)"
  if [[ -z "$pids" ]]; then
    return
  fi

  echo "Port ${PORT} is already in use. Stopping existing listener(s): $pids"
  kill $pids 2>/dev/null || true

  local attempts=20
  while [[ $attempts -gt 0 ]]; do
    if ! lsof -ti "tcp:${PORT}" -sTCP:LISTEN >/dev/null 2>&1; then
      echo "Port ${PORT} is now free."
      return
    fi
    sleep 0.25
    attempts=$((attempts - 1))
  done

  echo "Failed to free port ${PORT}. Please stop the process manually and retry." >&2
  exit 1
}

# Keep the Wasm dev server watching sources so browser refresh picks up changes
# without manually restarting the task.
stop_existing_dev_server
exec ./gradlew \
  -Pkotlin.user.home="$ROOT/.kotlin-home" \
  -Pkotlin.compiler.execution.strategy=in-process \
  --continuous \
  --no-build-cache \
  :composeApp:wasmJsBrowserDevelopmentRun \
  "$@"
