#!/bin/bash
set -euo pipefail

# Only run in remote (web) sessions
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Register committed git hooks so pre-push checks run in this session
git -C "${CLAUDE_PROJECT_DIR:-.}" config core.hooksPath .claude/hooks
