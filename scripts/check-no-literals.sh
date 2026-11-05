#!/bin/sh
# Fails when production code holds a literal user-facing text instead of a string resource (feature 032).
# Usage: check-no-literals.sh [ROOT]   (ROOT defaults to the repository root)
#
# What counts as a text: a string literal that starts with a capital letter followed by two lowercase letters
# ("Split", "No sound in this clip"). In core/model only sentences count (a capital word, a space, a lowercase word),
# because its enums use such words as stored names. Lines that contain a fragment listed in
# scripts/literals-allowed.txt are accepted (product names, stored identifiers, routes). Comments and imports are skipped.
root="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
allowed="$root/scripts/literals-allowed.txt"
skip='^[^:]+:[0-9]+:[[:space:]]*(//|\*|/\*|import |package )'

screens=$(find "$root" -path '*/src/main/*' -name '*.kt' -not -path '*/build/*' -not -path '*/core/model/*' \
  -not -path '*/benchmark/*' -not -path '*/build-logic/*' -print0 \
  | xargs -0 grep -nE '"([A-Z][a-z]+|[A-Z] [a-z]+)[^"]*"' 2>/dev/null || true)
# The engine's command labels (undo history names) are never shown, so only its voice split reasons are checked.
model=$(find "$root" -path '*/core/model/src/main/*' -name '*.kt' -not -path '*/build/*' \
  \( -not -path '*/core/model/src/main/*/engine/*' -o -name 'VoiceSplitCommands.kt' \) -print0 \
  | xargs -0 grep -nE '"[A-Z][a-z]+ [a-z]+[^"]*"|"[A-Z] [a-z]+[^"]*"' 2>/dev/null || true)

# Stored names (database), log lines, and annotations are not user text.
noise='(Log\.[a-z]\(|@Suppress|@ColumnInfo|const val TAG|/core/data/src/main/kotlin/[^:]*/db/)'
hits=$(printf '%s\n%s\n' "$screens" "$model" | sed '/^$/d' | grep -vE "$skip" | grep -vE "$noise" \
  | grep -v 'contentDescription = null' || true)
if [ -f "$allowed" ]; then
  hits=$(printf '%s\n' "$hits" | grep -vFf "$allowed" || true)
fi
hits=$(printf '%s\n' "$hits" | sed '/^$/d')

if [ -n "$hits" ]; then
  count=$(printf '%s\n' "$hits" | wc -l | tr -d ' ')
  printf '%s\n' "$hits" | sed "s#^$root/##"
  echo "check-no-literals: $count literal text(s) found; move them to string resources" >&2
  exit 1
fi
echo "check-no-literals: ok"
