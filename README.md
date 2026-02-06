# Automated Program Repair Tool (GenProg-Style)

Java implementation of a GenProg-style Automated Program Repair (APR) engine for Task 03.

## Team
- Group 7
- Tim Greller
- Jonas Heitz
- Rafail Venediktov

## 1. Overview
This tool takes a buggy Java program, a test suite, and fault-localization weights, then searches for a patch using a genetic programming loop.

High-level flow:
1. Load benchmark metadata and source files.
2. Build initial patch population.
3. Evaluate candidates by compile + test execution.
4. Select better candidates.
5. Generate offspring with crossover + mutation.
6. Repeat until all tests pass or limits are reached.

## 2. GenProg Mapping
Implemented pipeline is assignment-oriented and close to GenProg pseudocode:
- weighted fault localization guides mutation target choice
- patch population is evolved over generations
- fitness is test-based
- stop criteria: full test pass, generation cap, time cap

Core GenProg edit operators:
- `DELETE`
- `INSERT`
- `SWAP`

Additional generic expression-level operators (for practical Java repairability):
- `REPLACE_EXPR`
- `MUTATE_BINARY_OPERATOR`
- `NEGATE_EXPRESSION`

## 3. Fault Localization Policy (Strict)
The current repository behavior is strict with respect to FL weights:
- suspiciousness values come only from `fault-localization.json`
- expected levels are `1.0`, `0.1`, `0.0`
- no fallback exploration of `0.0` statements in statement selection
- no suspiciousness normalization in selection probability

## 4. Project Structure
```text
.
├── src/main/java/edu/passau/apr/
│   ├── Main.java                      # CLI entrypoint, run orchestration
│   ├── algorithm/
│   │   └── GeneticAlgorithm.java      # Evolution loop (init/select/crossover/mutate/evaluate)
│   ├── config/
│   │   └── Config.java                # Runtime parameters + defaults
│   ├── evaluator/
│   │   └── FitnessEvaluator.java      # Compile + JUnit execution + fitness calculation
│   ├── model/
│   │   ├── BenchmarkConfig.java       # Parsed benchmark metadata
│   │   ├── Edit.java                  # Edit representation
│   │   ├── FitnessResult.java         # Fitness tuple/state
│   │   ├── Patch.java                 # Patch object + mutation/apply logic
│   │   └── StatementWeight.java       # FL weight model
│   ├── operator/
│   │   ├── PatchGenerator.java        # Random/guided patch generation + crossover
│   │   └── PatchGeneratorTest.java    # Generator-focused tests
│   └── util/
│       ├── AstUtils.java              # Shared AST safety/compatibility checks
│       ├── BenchmarkLoader.java       # JSON loading for benchmark/fl config
│       └── Pair.java                  # Utility pair
├── benchmarks/                        # Benchmark suite B01..B12
├── docs/
│   ├── assignment/                    # Task PDF + declaration template
│   └── papers/                        # GenProg papers
├── test_quick.sh                      # Sweep script for all benchmarks
├── README.md
└── REPORT.md
```

## 5. Benchmarks
Current benchmark set:
- `B01_OffByOne`
- `B02_DuplicateLine`
- `B03_MissingStatement`
- `B04_WrongPredicates`
- `B05_WrongIndex`
- `B06_InventoryNormalization`
- `B07_IntervalPlanner`
- `B08_LogReportBuilder`
- `B09_ShippingTierSwitch`
- `B10_BillingProration`
- `B11_ConsecutiveBatcher`
- `B12_TagSanitizer`

Each benchmark directory contains:
- `buggy/` — buggy source
- `fixed/` — reference fixed source
- `tests/` — JUnit tests
- `benchmark.json` — paths + test class names
- `fault-localization.json` — suspiciousness weights

## 6. Requirements
- Java 17+
- macOS/Linux shell (examples use `bash`)
- Gradle Wrapper included (`./gradlew`)

## 7. Build and Run
### Build
```bash
./gradlew build
```

### Run one benchmark
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 80 --timeLimitSec 120" --no-daemon
```

### Run full sweep
```bash
./test_quick.sh
# or explicitly:
./test_quick.sh 42 80 120
```

## 8. CLI Options
- `--benchmark <path>` required
- `--seed <long>`
- `--maxGenerations <int>`
- `--timeLimitSec <long>`
- `--populationSize <int>`
- `--positiveTestWeight <double>`
- `--negativeTestWeight <double>`
- `--mutationWeight <double>`
- `--verbose`

Defaults are defined in `/Users/rafailvv/Учеба/University of Passau/Program Repair/Task 3/src/main/java/edu/passau/apr/config/Config.java`.

## 9. Output Layout
Patched source output:
- `out/<benchmark>/patch_<timestamp>/<MainClass>.java`

## 10. How to Read `test_quick.sh` Output
- `Status: OK` means the APR process exited with code `0`.
- Actual repair success is indicated by:
  - `SUCCESS: Found a patch that passes all tests!`
- `No solution found within limits.` means no full repair was found under given search budget.

## 11. Common Troubleshooting
- If runs are unstable between launches, keep seed fixed and increase `--maxGenerations` / `--timeLimitSec`.
- If runs look stuck, verify FL file points to correct buggy lines.
- If output is very verbose, redirect stderr or use filtered script output.
- If Gradle daemon state is noisy, use `--no-daemon` as in examples.

## 12. Notes
- APR is stochastic by design; outcomes can vary by seed and budget.
- The implementation remains benchmark-agnostic at algorithm level (no benchmark-specific hardcoded fixes).
