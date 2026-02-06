# Task 03 Project Report

## Administrative Information
- Course: Automated Program Repair
- Assignment: Task 03
- Group slot on Stud.IP: Group 7
- Group members:
  - Tim Greller
  - Jonas Heitz
  - Rafail Venediktov

## Contributions of Each Group Member
- Tim Greller
  - Co-designed benchmark tasks and expected bug/fix pairs.
  - Worked on benchmark validation and fault-localization files.
  - Reviewed benchmark realism and diversity of bug classes.
- Jonas Heitz
  - Worked on documentation quality and consistency checks.
  - Helped validate assignment compliance and report structure.
  - Reviewed reproducibility steps and benchmark creation guidance.
- Rafail Venediktov
  - Implemented and refactored APR core (GA loop, patch model, operators, evaluator).
  - Implemented GenProg-style crossover/mutation behavior and strict FL behavior.
  - Integrated/maintained benchmark sweep workflow and runtime diagnostics.

All members participated in implementation discussion, benchmark design decisions, and report preparation.

## 1. Project Overview
We implemented a Java APR tool with a GenProg-style evolutionary workflow: generate patches, evaluate by tests, select fitter candidates, produce offspring via crossover and mutation, and iterate until either all tests pass or a resource budget is exhausted.

## 2. Project Structure and Where to Find Components

### 2.1 Source Code Structure
- Entry point
  - `src/main/java/edu/passau/apr/Main.java`
- Algorithm loop
  - `src/main/java/edu/passau/apr/algorithm/GeneticAlgorithm.java`
- Configuration
  - `src/main/java/edu/passau/apr/config/Config.java`
- Fitness evaluation (compile + run tests)
  - `src/main/java/edu/passau/apr/evaluator/FitnessEvaluator.java`
- Core data models
  - `src/main/java/edu/passau/apr/model/Patch.java`
  - `src/main/java/edu/passau/apr/model/Edit.java`
  - `src/main/java/edu/passau/apr/model/FitnessResult.java`
  - `src/main/java/edu/passau/apr/model/BenchmarkConfig.java`
- Candidate generation and crossover
  - `src/main/java/edu/passau/apr/operator/PatchGenerator.java`
- Utilities
  - `src/main/java/edu/passau/apr/util/BenchmarkLoader.java`
  - `src/main/java/edu/passau/apr/util/AstUtils.java`
  - `src/main/java/edu/passau/apr/util/Pair.java`

### 2.2 Benchmark Layout
Each benchmark directory in `benchmarks/` contains:
- `buggy/` buggy source
- `fixed/` fixed reference source
- `tests/` JUnit tests
- `benchmark.json` benchmark metadata
- `fault-localization.json` suspiciousness weights

Implemented benchmark set:
- B01_OffByOne
- B02_DuplicateLine
- B03_MissingStatement
- B04_WrongPredicates
- B05_WrongIndex
- B06_InventoryNormalization
- B07_IntervalPlanner
- B08_LogReportBuilder
- B09_ShippingTierSwitch
- B10_BillingProration
- B11_ConsecutiveBatcher
- B12_TagSanitizer

### 2.3 Tooling Files
- Quick benchmark sweep script: `test_quick.sh`
- Build and dependencies: `build.gradle`

## 3. Implementation Summary

### 3.1 GenProg-Style Workflow
The implementation follows the expected structure:
1. Read benchmark metadata and FL weights.
2. Generate initial candidate patch population.
3. Evaluate each candidate by compiling patched program and running tests.
4. Select fitter candidates (tournament style).
5. Create offspring by one-point crossover + mutation.
6. Repeat until success or budget limits.

### 3.2 Edit Operators
Patch representation is statement/expression based with these operators:
- `DELETE`
- `INSERT`
- `SWAP`
- `REPLACE_EXPR`
- `MUTATE_BINARY_OPERATOR`
- `NEGATE_EXPRESSION`

Core GenProg operators are `DELETE/INSERT/SWAP`; expression-level operators are generic extensions to handle realistic Java expression bugs.

### 3.3 Fault Localization Behavior
Strict assignment-oriented FL mode is implemented:
- weights are taken directly from `fault-localization.json`
- statements with weight `0.0` are not selected as mutation targets
- no normalization of FL weights for target selection

### 3.4 Fitness Evaluation
A candidate patch is scored after compile + tests. Compilation/test failures reduce or invalidate fitness. The evaluator enforces timeouts and isolates class loading.

## 4. Instructions to Run the Project (Section 2.2)

### 4.1 Requirements
- Java 17+
- Gradle wrapper (already included)

### 4.2 Build
```bash
./gradlew build
```

### 4.3 Run a Single Benchmark
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 80 --timeLimitSec 120" --no-daemon
```

### 4.4 Run All Benchmarks
```bash
./test_quick.sh 42 80 120
```

### 4.5 Important Output Interpretation
In `test_quick.sh` output:
- `Status: OK` means the run process exited successfully.
- Actual repair success is indicated by `SUCCESS: Found a patch that passes all tests!`.

## 5. Benchmark Information and How to Create New Benchmarks

### 5.1 Benchmark Characteristics
Our benchmarks cover different bug classes, including:
- off-by-one and wrong index
- duplicate/missing statement behavior
- wrong predicate/condition
- wrong sign/arithmetic/logical behavior
- collection/list handling side effects
- output normalization and ordering mistakes

### 5.2 Number of Tests per Benchmark
- B01: 6
- B02: 8
- B03: 8
- B04: 8
- B05: 8
- B06: 7
- B07: 5
- B08: 5
- B09: 5
- B10: 5
- B11: 5
- B12: 5

### 5.3 How to Create a New Benchmark (Consistent with Assumptions)
1. Create folder `benchmarks/BXX_Name/`.
2. Add:
   - `buggy/<MainClass>.java`
   - `fixed/<MainClass>.java`
   - `tests/<MainClass>Test.java`
   - `benchmark.json`
   - `fault-localization.json`
3. In `benchmark.json`, set:
   - `buggySourcePath`, `fixedSourcePath`, `testSourcePath`, `faultLocalizationPath`, `mainClassName`, `testClassNames`
4. In `fault-localization.json`, assign suspiciousness values per line using assignment scheme (typically `1`, `0.1`, `0`).
5. Validate benchmark manually:
   - buggy version should fail at least one test
   - fixed version should pass all tests
6. Run APR tool on the new benchmark:
```bash
./gradlew run --args="--benchmark benchmarks/BXX_Name --seed 42 --maxGenerations 80 --timeLimitSec 120" --no-daemon
```

## 6. Section 2.1 Questions

### 1. What were the most challenging parts of implementing this project?
The hardest parts were keeping the search close to GenProg semantics while still making it robust in Java: reliable compile/test isolation, crossover index consistency after insert/delete edits, controlling invalid mutants, and balancing strict fault-localization constraints against search effectiveness.

### 2. Does your implementation run and behave as expected? If not, what was the issue?
Yes, the current implementation runs end-to-end and behaves as expected in our reference run; during development we observed cases where strict FL plus weak/incorrect FL lines prevented progress, and this was resolved by correcting benchmark FL mappings and stabilizing mutation/crossover behavior.

### 3. Did your tool manage to fix any benchmarks? If yes, which ones, and include the patch or patches.
Yes. In the reference sweep (`./test_quick.sh 42 80 120`), all 12 benchmarks were fixed.

Reference run summary (from `/tmp/task3_quick_42_80_120.log`):
- B01: solved in generation 0
- B02: solved in generation 3
- B03: solved in generation 9
- B04: solved in generation 74
- B05: solved in generation 0
- B06: solved in generation 0
- B07: solved in generation 0
- B08: solved in generation 0
- B09: solved in generation 0
- B10: solved in generation 0
- B11: solved in generation 0
- B12: solved in generation 10

Example patches (buggy -> fixed diffs):
- B01_OffByOne: `while (i <= numbers.length)` -> `while (i < numbers.length)`
- B02_DuplicateLine: removed duplicated `count++;`
- B03_MissingStatement: `System.out.println("Append here")` -> `result.append(", ")`
- B04_WrongPredicates: removed contradictory null-check and removed `+ "bug"` in mapping
- B05_WrongIndex: fixed first/last element index returns (`0` vs `array.length - 1`)
- B06_InventoryNormalization: outbound delta sign fixed (`event.delta` -> `-event.delta`)
- B07_IntervalPlanner: `return intervals` -> `return merged`
- B08_LogReportBuilder: comparator condition fixed (`latencyCmp == 0` -> `latencyCmp != 0`)
- B09_ShippingTierSwitch: weekend condition fixed (`== false` -> `!= false`)
- B10_BillingProration: removed incorrect overwrite `boundedActiveDays = daysInMonth;`
- B11_ConsecutiveBatcher: store copy before `current.clear()` (`new ArrayList<>(current)`)
- B12_TagSanitizer: truncation guard fixed (`length() < 20` -> `length() > 20`)

### 4. Were there any benchmarks you created that were not fixed by your tool? If yes, what was the reason?
In the referenced run with seed 42 and the given budget, none remained unfixed; however, because APR is stochastic, other seeds or stricter budgets can still leave difficult cases unsolved within limits.

### 5. What are the limitations of your implementation (e.g., which simplifying assumptions did you make)?
Main limitations/assumptions:
- single benchmark target at a time (single source entry from benchmark metadata)
- benchmark-provided FL (no dynamic FL generation in the tool)
- stochastic search quality depends on seed and budget
- bounded mutation/edit budgets to control runtime
- simplified runtime model compared to full industrial APR pipelines

### 6. How did you test your tool?
We tested with:
- unit-level checks for generator logic where applicable
- repeated single-benchmark runs with fixed seeds
- full benchmark sweeps via `test_quick.sh`
- verification that produced patched files compile and correspond to intended bug fixes
- regression checks after algorithmic changes (especially crossover, strict FL behavior, and mutation target selection)

## 7. Additional Notes for Submission
- This report is written in Markdown source and should be exported to PDF for submission.
- The final PDF must satisfy the assignment constraint (maximum 20 pages of content).
- Include the signed originality declaration as required by the assignment.
