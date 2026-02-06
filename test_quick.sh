#!/usr/bin/env bash

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

for bench in "${BENCHMARKS[@]}"; do
  echo "===== $bench ====="
  ./gradlew run \
    --args="--benchmark benchmarks/$bench --seed $SEED --maxGenerations $MAX_GENERATIONS --timeLimitSec $TIME_LIMIT_SEC" \
    --no-daemon 2>/dev/null \
    | grep -E "Generation [0-9]+:|=== Results ===|Generations:|Time:|SUCCESS:|No solution|Best fitness:|Patched file saved to" || true
  echo
done

echo "=== Sweep Complete ==="
