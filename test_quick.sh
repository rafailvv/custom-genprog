#!/bin/bash

# Quick test script for APR tool
# Tests all benchmarks with short parameters

echo "=== Quick Test of APR Tool ==="
echo ""

BENCHMARKS=("B01_OffByOne" "B02_WrongOperator" "B03_WrongCondition" "B04_MissingStatement" "B05_WrongVariable")

for bench in "${BENCHMARKS[@]}"; do
    echo "Testing $bench..."
    echo "----------------------------------------"
    
    ./gradlew run --args="--benchmark benchmarks/$bench --seed 42 --maxGenerations 5 --timeLimitSec 15" --no-daemon 2>&1 | \
        grep -E "SUCCESS|No solution|Generation [0-9]+:|Results|Best fitness" | head -5
    
    echo ""
    sleep 1
done

echo "=== Test Complete ==="
