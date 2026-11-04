#!/bin/sh
# Tests for changelog-notes.sh. Run from anywhere: scripts/test-changelog-notes.sh
here=$(cd "$(dirname "$0")" && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
fails=0

check() {
  if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1"; echo "  expected: $2"; echo "  got:      $3"; fails=$((fails + 1)); fi
}

cat > "$tmp/CHANGELOG.md" <<'TXT'
# Changelog

## [Unreleased]

## [0.2.0]

### Added
- A thing with `code` and [a link](https://example.com).

### Fixed
- Another thing.

## [0.1.0] - 2026-01-01

### Added
- First.

## [0.0.9]

## [0.0.8]

### Added
- Older.
TXT

printf '### Added\n- A thing with `code` and [a link](https://example.com).\n\n### Fixed\n- Another thing.\n' > "$tmp/expected-0.2.0"
sh "$here/changelog-notes.sh" 0.2.0 "$tmp/CHANGELOG.md" > "$tmp/got-0.2.0" 2>/dev/null
check "prints a section unchanged and stops at the next heading" "$(cat "$tmp/expected-0.2.0")" "$(cat "$tmp/got-0.2.0")"

sh "$here/changelog-notes.sh" 0.1.0 "$tmp/CHANGELOG.md" > "$tmp/got-0.1.0" 2>/dev/null
check "finds a heading that has a date" "### Added
- First." "$(cat "$tmp/got-0.1.0")"

sh "$here/changelog-notes.sh" 9.9.9 "$tmp/CHANGELOG.md" > /dev/null 2> "$tmp/err"
check "fails for a version that is not there" "1" "$?"
check "says which version is missing" "No section for version 9.9.9 in $tmp/CHANGELOG.md" "$(cat "$tmp/err")"

sh "$here/changelog-notes.sh" 0.0.9 "$tmp/CHANGELOG.md" > /dev/null 2> "$tmp/err"
check "fails for an empty section" "1" "$?"
check "says the section is empty" "Section for version 0.0.9 in $tmp/CHANGELOG.md is empty" "$(cat "$tmp/err")"

sh "$here/changelog-notes.sh" 0.0.8 "$tmp/CHANGELOG.md" > "$tmp/got-last" 2>/dev/null
check "reads the last section to the end of the file" "### Added
- Older." "$(cat "$tmp/got-last")"

[ "$fails" -eq 0 ] && echo "all passed" || { echo "$fails failed"; exit 1; }
