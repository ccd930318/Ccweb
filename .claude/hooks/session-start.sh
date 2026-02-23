#!/bin/bash
set -euo pipefail

# Only run in Claude Code remote (web) environments
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "==> Installing frontend dependencies..."
cd "$CLAUDE_PROJECT_DIR/frontend"
npm install

echo "==> Downloading Gradle wrapper (backend)..."
cd "$CLAUDE_PROJECT_DIR/backend"
./gradlew dependencies --no-daemon -q 2>&1 | tail -3 || true

echo "==> Session start hook complete."
