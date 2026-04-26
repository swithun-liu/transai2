#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

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

exec ./gradlew \
  -Pkotlin.user.home="$ROOT/.kotlin-home" \
  -Pkotlin.compiler.execution.strategy=in-process \
  --no-build-cache \
  :composeApp:wasmJsBrowserDevelopmentRun \
  "$@"
