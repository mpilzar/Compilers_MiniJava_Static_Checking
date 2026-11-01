#!/usr/bin/env bash
#
# Regression suite for the MiniJava semantic analyzer.
#
#   tests/valid/*.java     must be accepted (exit 0) and print exactly tests/expected/<name>.txt
#   tests/invalid/*.java   must be rejected (exit 1) with a "file:line:col: error:" diagnostic;
#                          the line carrying a "// ERROR" marker is where it must be reported
#
# Usage: tests/run_tests.sh [-v] [--javac]
#   -v       list every test, not just failures
#   --javac  also compile every valid program with javac: MiniJava is a subset of Java,
#            so anything this analyzer accepts must be a legal Java program too

set -u
cd "$(dirname "$0")/.."

JAR=build/minijava-checker.jar
VERBOSE=0
WITH_JAVAC=0
for arg in "$@"; do
    case "$arg" in
        -v) VERBOSE=1 ;;
        --javac) WITH_JAVAC=1 ;;
        *) echo "usage: $0 [-v] [--javac]" >&2; exit 2 ;;
    esac
done

if [ ! -f "$JAR" ]; then
    echo "error: $JAR not found - run 'make' first" >&2
    exit 2
fi

if [ -t 1 ]; then GREEN=$'\033[32m'; RED=$'\033[31m'; BOLD=$'\033[1m'; RESET=$'\033[0m'
else GREEN=""; RED=""; BOLD=""; RESET=""; fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

total=0
failed=0

pass() { total=$((total + 1)); [ $VERBOSE -eq 1 ] && printf '  %sok%s    %s\n' "$GREEN" "$RESET" "$1"; return 0; }
fail() {
    total=$((total + 1)); failed=$((failed + 1))
    printf '  %sFAIL%s  %s\n' "$RED" "$RESET" "$1"
    [ -n "${2:-}" ] && printf '%s\n' "$2" | sed 's/^/        /'
    return 0
}

run_checker() { # $1 = file; writes $TMP/out and $TMP/err; returns exit status
    java -jar "$JAR" "$1" > "$TMP/out" 2> "$TMP/err.raw"
    local status=$?
    grep -v '^Picked up JAVA_TOOL_OPTIONS' "$TMP/err.raw" > "$TMP/err"
    return $status
}

# ---------------------------------------------------------------- valid programs
echo "${BOLD}Valid programs${RESET} (accepted, offsets match expected output)"
valid_before=$failed; valid_count=0
for src in tests/valid/*.java; do
    name=$(basename "$src" .java)
    expected="tests/expected/$name.txt"
    valid_count=$((valid_count + 1))
    run_checker "$src"; status=$?
    if [ $status -ne 0 ]; then
        fail "$name" "expected exit 0, got $status: $(head -1 "$TMP/err")"
    elif [ ! -f "$expected" ]; then
        fail "$name" "missing $expected"
    elif ! diff -q "$expected" "$TMP/out" > /dev/null; then
        fail "$name" "$(diff "$expected" "$TMP/out" | head -12)"
    else
        pass "$name"
    fi
done
printf '  %d/%d passed\n\n' $((valid_count - (failed - valid_before))) $valid_count

# ---------------------------------------------------------------- invalid programs
echo "${BOLD}Invalid programs${RESET} (rejected, error reported on the marked line)"
invalid_before=$failed; invalid_count=0
for src in tests/invalid/*.java; do
    name=$(basename "$src" .java)
    invalid_count=$((invalid_count + 1))
    marker_line=$(grep -n '// ERROR' "$src" | head -1 | cut -d: -f1)
    run_checker "$src"; status=$?
    diag=$(grep -m1 -E "^.+:[0-9]+:[0-9]+: error: " "$TMP/err")
    if [ $status -ne 1 ]; then
        fail "$name" "expected exit 1, got $status"
    elif [ -n "$(cat "$TMP/out")" ]; then
        fail "$name" "rejected file must not print offsets"
    elif [ -z "$diag" ]; then
        fail "$name" "no 'file:line:col: error:' diagnostic in: $(head -1 "$TMP/err")"
    elif [ -z "$marker_line" ]; then
        fail "$name" "test file has no '// ERROR' marker"
    else
        reported_line=$(printf '%s' "$diag" | sed -E 's/^.*\.java:([0-9]+):[0-9]+: error: .*$/\1/')
        if [ "$reported_line" != "$marker_line" ]; then
            fail "$name" "error reported on line $reported_line, expected line $marker_line"$'\n'"$diag"
        else
            pass "$name  ->  ${diag#*error: }"
        fi
    fi
done
printf '  %d/%d passed\n\n' $((invalid_count - (failed - invalid_before))) $invalid_count

# ---------------------------------------------------------------- multi-file invocation
echo "${BOLD}Command line${RESET}"
java -jar "$JAR" tests/valid/*.java > "$TMP/multi.out" 2> /dev/null; status=$?
headers=$(grep -c '^==> .* <==$' "$TMP/multi.out")
if [ $status -eq 0 ] && [ "$headers" -eq "$valid_count" ]; then
    pass "all valid files in one run (exit 0, $headers file headers)"
else
    fail "all valid files in one run" "exit $status, $headers headers for $valid_count files"
fi

java -jar "$JAR" tests/valid/Factorial.java tests/invalid/BadAssign.java tests/valid/BinaryTree.java \
    > "$TMP/mixed.out" 2> "$TMP/mixed.err"; status=$?
if [ $status -eq 1 ] && grep -q '^BinaryTree\|^-----------Class Tree' "$TMP/mixed.out" \
        && grep -q 'BadAssign.java:6:' "$TMP/mixed.err"; then
    pass "an invalid file does not stop later files (exit 1)"
else
    fail "an invalid file does not stop later files" "exit $status"
fi

java -jar "$JAR" > /dev/null 2>&1; status=$?
if [ $status -eq 2 ]; then pass "no arguments prints usage (exit 2)"; else fail "no arguments prints usage" "exit $status"; fi

java -jar "$JAR" tests/does-not-exist.java > /dev/null 2>&1; status=$?
if [ $status -eq 1 ]; then pass "missing file is reported (exit 1)"; else fail "missing file is reported" "exit $status"; fi
echo

# ---------------------------------------------------------------- javac oracle (optional)
if [ $WITH_JAVAC -eq 1 ]; then
    echo "${BOLD}javac cross-check${RESET} (every accepted program is legal Java)"
    javac_before=$failed
    for src in tests/valid/*.java; do
        name=$(basename "$src" .java)
        mkdir -p "$TMP/javac/$name"
        if javac -nowarn -d "$TMP/javac/$name" "$src" > "$TMP/javac.log" 2>&1; then
            pass "javac $name"
        else
            fail "javac $name" "$(grep -v '^Picked up' "$TMP/javac.log" | head -5)"
        fi
    done
    printf '  %d/%d compiled\n\n' $((valid_count - (failed - javac_before))) $valid_count
fi

# ---------------------------------------------------------------- summary
if [ $failed -eq 0 ]; then
    echo "${GREEN}${BOLD}All $total tests passed.${RESET}"
else
    echo "${RED}${BOLD}$failed of $total tests failed.${RESET}"
    exit 1
fi
