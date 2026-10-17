#!/usr/bin/env bash
# PreToolUse hook (Edit|Write): blocks changes to Flyway migrations that already exist on origin/main.
# Flyway stores a checksum of every applied migration; editing one makes existing databases refuse to start.
# Exit 2 blocks the tool call and shows stderr to Claude. Any other failure lets the call through.

input="$(cat)"

if command -v jq >/dev/null 2>&1; then
  file_path="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty')"
elif command -v python3 >/dev/null 2>&1; then
  file_path="$(printf '%s' "$input" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("tool_input",{}).get("file_path",""))')"
else
  exit 0
fi

case "$file_path" in
  */src/main/resources/db/migration/*.sql) ;;
  *) exit 0 ;;
esac

repo_root="$(git -C "$(dirname "$file_path")" rev-parse --show-toplevel 2>/dev/null)" || exit 0
rel_path="${file_path#"$repo_root"/}"

if git -C "$repo_root" cat-file -e "origin/main:$rel_path" 2>/dev/null; then
  cat >&2 <<EOF
Blocked: $rel_path is already on origin/main. Flyway keeps a checksum of applied migrations, so editing it
breaks every database that already ran it. Add a new migration (V<next>__<description>.sql) instead.
See docs/database.md. If this migration was never applied to any shared database and must be fixed in place,
ask a maintainer to make that change by hand.
EOF
  exit 2
fi

exit 0
