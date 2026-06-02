#!/usr/bin/env bash
# roborazzi-compare.sh — run a Roborazzi screenshot task for ONE screen and surface
# the diff image path(s) so an agent/vision step can read them.
#
# Usage:
#   scripts/roborazzi-compare.sh <TestFilter> [module] [mode]
#     TestFilter   --tests glob, e.g. "*LoginScreenScreenshotTest"   (required)
#     module       gradle module path                                (default: ":app")
#     mode         compare | verify | record                         (default: compare)
#
# Modes (run on the STANDARD test task so --tests is honored; the *Roborazzi* wrapper
# tasks do not reliably forward --tests):
#   compare  calibration: diff against current golden, NEVER fails on diff, emits *_compare.png
#   verify   guard/CI:    diff against golden, FAILS (non-zero) on any delta past threshold
#   record   lock the current render as the new golden (use after human says "ok")
#
# Output (machine-readable, last lines):
#   THRESHOLD_MODE=<mode>
#   RESULT=<pass|diff|recorded|error>
#   DIFF_IMAGES<<EOF ... EOF   (only when RESULT=diff)
#
# Exit code mirrors gradle: 0 = matched / recorded, non-zero = diff (verify) or build failure.
set -uo pipefail

FILTER="${1:?need a --tests filter, e.g. '*LoginScreenScreenshotTest'}"
MODULE="${2:-:app}"
MODE="${3:-compare}"

case "$MODE" in
  compare) PROP="-Proborazzi.test.compare=true" ;;
  verify)  PROP="-Proborazzi.test.verify=true" ;;
  record)  PROP="-Proborazzi.test.record=true" ;;
  *) echo "RESULT=error"; echo "unknown mode: $MODE (use compare|verify|record)" >&2; exit 2 ;;
esac

# ":app" -> "app", ":feature:login" -> "feature/login"
OUT_DIR="${MODULE#:}"; OUT_DIR="${OUT_DIR//://}/build/outputs/roborazzi"

echo "THRESHOLD_MODE=$MODE"

# Clear stale diffs so anything found AFTER the run is genuinely from this run
# (more reliable than an mtime comparison).
if [ -d "$OUT_DIR" ]; then
  find "$OUT_DIR" -name '*_compare.png' -delete 2>/dev/null || true
fi

./gradlew "${MODULE}:testDebugUnitTest" --tests "$FILTER" "$PROP"
GRADLE_RC=$?

DIFFS=""
if [ -d "$OUT_DIR" ]; then
  DIFFS="$(find "$OUT_DIR" -name '*_compare.png' 2>/dev/null || true)"
fi

if [ "$MODE" = "record" ]; then
  echo "RESULT=recorded"
elif [ -n "$DIFFS" ]; then
  echo "RESULT=diff"
  echo "DIFF_IMAGES<<EOF"
  echo "$DIFFS"
  echo "EOF"
elif [ "$GRADLE_RC" -eq 0 ]; then
  echo "RESULT=pass"
else
  echo "RESULT=error"
fi

exit "$GRADLE_RC"
