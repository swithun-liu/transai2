#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

cd "$ROOT"

echo "🚀 Preparing TransAI Reader for EdgeOne Pages..."
"$ROOT/prepare-web-dist.sh"

echo
echo "✅ EdgeOne deployment files are ready."
echo "📁 Static output: dist/"
echo "⚙️ EdgeOne config: edgeone.json"
echo "🧩 Pages Function: functions/api/chat/completions.js"
echo
echo "Next step:"
echo "1. Commit and push the repository."
echo "2. Import the repo in EdgeOne Pages."
echo "3. Verify output directory is dist and function route /api/chat/completions works."
