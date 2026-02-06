#!/usr/bin/env bash

set -u
set -o pipefail

# Quick benchmark sweep for the APR tool.
# Usage: ./test_quick.sh [seed] [maxGenerations] [timeLimitSec]

SEED="${1:-42}"
MAX_GENERATIONS="${2:-80}"
TIME_LIMIT_SEC="${3:-120}"

BENCHMARKS=(
  "B01_OffByOne"
  "B02_DuplicateLine"
  "B03_MissingStatement"
  "B04_WrongPredicates"
  "B05_WrongIndex"
  "B06_InventoryNormalization"
  "B07_IntervalPlanner"
  "B08_LogReportBuilder"
)

echo "=== Quick APR Benchmark Sweep ==="
echo "Seed: $SEED | MaxGenerations: $MAX_GENERATIONS | TimeLimitSec: $TIME_LIMIT_SEC"
echo

PASS_COUNT=0
FAIL_COUNT=0

for bench in "${BENCHMARKS[@]}"; do
  echo "===== $bench ====="
  tmp_log="$(mktemp)"

  ./gradlew run \
    --args="--benchmark benchmarks/$bench --seed $SEED --maxGenerations $MAX_GENERATIONS --timeLimitSec $TIME_LIMIT_SEC" \
    --no-daemon >"$tmp_log" 2>&1
  cmd_status=$?

  filtered_output="$(grep -E "Generation [0-9]+:|=== Results ===|Generations:|Time:|SUCCESS:|No solution|Best fitness:|Patched file saved to|Error:|Exception|BUILD FAILED" "$tmp_log" || true)"
  if [[ -n "$filtered_output" ]]; then
    printf "%s\n" "$filtered_output"
  else
    echo "No filtered progress lines found. Last 20 log lines:"
    tail -n 20 "$tmp_log"
  fi

  if [[ $cmd_status -eq 0 ]]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    echo "Status: OK"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    echo "Status: FAILED (exit code $cmd_status)"
  fi

  rm -f "$tmp_log"
  echo
done

echo "Summary: passed=$PASS_COUNT failed=$FAIL_COUNT total=${#BENCHMARKS[@]}"
echo "=== Sweep Complete ==="
