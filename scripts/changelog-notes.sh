#!/bin/sh
# Prints the section of CHANGELOG.md for one version, without its heading and without blank lines around it.
# Usage: changelog-notes.sh VERSION [CHANGELOG_FILE]
# Fails (exit 1, one line on stderr) when the version has no section or the section is empty.
version="$1"
file="${2:-CHANGELOG.md}"
if [ -z "$version" ]; then
  echo "Usage: changelog-notes.sh VERSION [CHANGELOG_FILE]" >&2
  exit 2
fi
if [ ! -f "$file" ]; then
  echo "Changelog file $file not found" >&2
  exit 1
fi

found=$(awk -v v="$version" 'index($0, "## [" v "]") == 1 { print "yes"; exit }' "$file")
if [ -z "$found" ]; then
  echo "No section for version $version in $file" >&2
  exit 1
fi

notes=$(awk -v v="$version" '
  index($0, "## [" v "]") == 1 { on = 1; next }
  on && index($0, "## [") == 1 { exit }
  on { print }
' "$file" | awk 'NF { started = 1 } started { lines[++n] = $0 } END { while (n > 0 && lines[n] ~ /^[ \t]*$/) n--; for (i = 1; i <= n; i++) print lines[i] }')

if [ -z "$notes" ]; then
  echo "Section for version $version in $file is empty" >&2
  exit 1
fi
printf '%s\n' "$notes"
