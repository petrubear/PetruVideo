#!/bin/sh
# Tests for check-no-literals.sh
here=$(cd "$(dirname "$0")" && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/m/src/main/kotlin" "$tmp/scripts"
fails=0
check() { if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1 (expected $2, got $3)"; fails=$((fails + 1)); fi; }

printf 'fun a() { Text(stringResource(R.string.x)); Icon(contentDescription = null) }\n' > "$tmp/m/src/main/kotlin/A.kt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "resources pass" 0 $?

printf 'fun a() { Text("Hello") }\n' > "$tmp/m/src/main/kotlin/A.kt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "a literal Text fails" 1 $?

printf 'fun a() { Icon(contentDescription = "Close") }\n' > "$tmp/m/src/main/kotlin/A.kt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "a literal description fails" 1 $?

printf 'fun a() { say("Split cancelled") }\n' > "$tmp/m/src/main/kotlin/A.kt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "a literal message fails" 1 $?

printf 'fun a() { Text("PetruVideo") }\n' > "$tmp/m/src/main/kotlin/A.kt"
printf 'Text("PetruVideo")\n' > "$tmp/scripts/literals-allowed.txt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "an allow-listed text passes" 0 $?

printf 'fun a() { Text("%%d", 1) }\n' > "$tmp/m/src/main/kotlin/A.kt"
rm "$tmp/scripts/literals-allowed.txt"
sh "$here/check-no-literals.sh" "$tmp" > /dev/null 2>&1; check "a number-only text passes" 0 $?

[ "$fails" -eq 0 ] && echo "all passed" || { echo "$fails failed"; exit 1; }
