#!/bin/bash
set -euo pipefail

# Only run in remote (web) sessions
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

BRANCH=$(git -C "${CLAUDE_PROJECT_DIR:-.}" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                  ⚠  AGENTS.md REMINDER  ⚠                      ║"
echo "╠══════════════════════════════════════════════════════════════════╣"
echo "║  Current branch: $BRANCH"
echo "║                                                                  ║"
echo "║  REQUIRED before writing any code:                               ║"
echo "║  1. Read AGENTS.md                                               ║"
echo "║  2. Check PR state: gh pr view --head $BRANCH                    ║"
echo "║  3. If PR is merged or closed → branch from master instead       ║"
echo "║     (new branch name: cursor/description-XXXX)                   ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Attempt to check PR state via GitHub API (works if token is available)
REPO_OWNER="gaborbiro"
REPO_NAME="DailyMacros"

if [ -n "${GITHUB_TOKEN:-}" ]; then
  RESPONSE=$(curl -s -H "Authorization: Bearer $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/pulls?head=$REPO_OWNER:$BRANCH&state=all&per_page=1" 2>/dev/null || echo "[]")
  STATE=$(echo "$RESPONSE" | grep -o '"state":"[^"]*"' | head -1 | cut -d'"' -f4)
  MERGED=$(echo "$RESPONSE" | grep -o '"merged":true' | head -1)

  if [ "$MERGED" = '"merged":true' ] || [ "$STATE" = "closed" ]; then
    echo "🚨 WARNING: The PR for branch '$BRANCH' is already MERGED/CLOSED."
    echo "   Do NOT push to this branch. Create a new branch from master."
    echo ""
  elif [ "$STATE" = "open" ]; then
    echo "✅ PR for branch '$BRANCH' is OPEN — safe to continue on this branch."
    echo ""
  fi
fi
