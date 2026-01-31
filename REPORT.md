# Project Report — Automated Program Repair Tool (GenProg-Style)

Date: 2026-01-31  
Course: Automated Program Repair  
Task: Task 3 — Project and Report

## Group Information
- Group slot (Stud.IP): Group 7
- Members: Tim Greller, Jonas Heitz, Rafail Venediktov

## Contributions (update if needed)
| Member | Contributions |
| --- | --- |
| Rafail Venediktov | Core implementation (genetic algorithm, fitness evaluation, CLI), integration, benchmark integration |
| Tim Greller | Benchmarks and tests, fault-localization weights, validation of expected fixes |
| Jonas Heitz | Documentation, assumptions/limitations write-up, scripts/README |

## Project Structure
Key locations:
- `src/main/java/edu/passau/apr/Main.java` — entry point, argument parsing, orchestration
- `src/main/java/edu/passau/apr/algorithm/GeneticAlgorithm.java` — GenProg-style main loop
- `src/main/java/edu/passau/apr/operator/` — DELETE/INSERT/REPLACE operators and patch generator
- `src/main/java/edu/passau/apr/evaluator/FitnessEvaluator.java` — compilation and test execution
- `src/main/java/edu/passau/apr/util/` — benchmark loader and weighted statement selector
- `benchmarks/` — five benchmarks (buggy/fixed/tests/configs)
- `docs/implementation-assumptions.md` — simplifying assumptions and limitations

## Implementation Overview (GenProg Mapping)
The tool follows the high-level GenProg pseudocode:
1. **Fault localization** — statement weights are provided per benchmark in `fault-localization.json`. Selection uses weights 1 / 0.1 / 0.0 as required by the assignment.
2. **Population initialization** — random patches of 1–3 edits (line-based).
3. **Search loop** — evaluate fitness by compiling patched code and running tests; stop when all tests pass or limits are reached.
4. **Selection & variation** — tournament selection (size 3), crossover and mutation to generate new patches.
5. **Result** — best patch is printed and saved to `out/<benchmark>/patch_<timestamp>/`.

This is a simplified, single-file Java version of GenProg that trades precision (AST-based edits, advanced fault localization) for a compact, understandable implementation suitable for the course task. The implementation keeps the original structure of GenProg (localize → generate → test → select → repeat) but uses line-level edits and precomputed localization weights to keep the tool light-weight and predictable for the given benchmarks.

### Patch Representation and Operators (Internal Details)
Patches are represented as a list of **Edit** objects. Each edit records:
- **Type** (DELETE / INSERT / REPLACE)
- **Line number** (1-based, as used in the benchmark files)
- **Content** (only for INSERT and REPLACE)

This representation is intentionally simple and file-based. It avoids parsing the AST and instead edits the source text directly. This reduces implementation complexity but can generate syntactically invalid patches, which are filtered out by the compilation step.

Each genetic operator works as follows:
- **DELETE**: selects a line using the weighted path selector and removes it.
- **INSERT**: selects a target line and inserts a donor line above it.
- **REPLACE**: selects a target line and replaces it with a donor line.

Donor statements are chosen uniformly from all source lines. This is a straightforward strategy that keeps the search space broad, but it is not context-aware (i.e., it does not yet prefer donors from the same method). To keep patch sizes bounded, each patch is limited to **at most three edits**. When crossover or mutation produces more edits, the extra edits are trimmed.

When applying a patch, edits are sorted in **descending line order**. This prevents index shifts from affecting later edits (for example, deleting a line changes all line numbers after it). The same patch-application logic is used both during fitness evaluation and when saving a final patch.

### Genetic Algorithm Configuration
Default parameters (match the recommended values from the assignment):
- Population size: 40  
- Positive test weight: 1.0  
- Negative test weight: 10.0  
- Mutation weight: 0.06  
- Crossover rate: 0.5  
- Tournament size: 3  
- Stop conditions: max generations, time limit, or all tests pass

### Fitness Evaluation
Fitness is computed as:
```
fitness = WPosT * (#passing) - WNegT * (#failing)
```
Details:
- Compilation via `javax.tools.JavaCompiler` (3s timeout).
- Tests executed via reflection by scanning `@Test` annotations (5s per test method).
- Each fitness evaluation is capped at 10 seconds.
- Tests are precompiled once; when a fixed version exists, it is used to compile tests.

#### Fitness Pipeline (Step-by-Step)
1. **Patch application**: the selected patch is applied to the buggy source file in memory.
2. **Temporary workspace**: a temporary directory is created for this evaluation.
3. **Compilation**: the patched source file is compiled into the temp class output directory.
4. **Test execution**: tests are run using a fresh class loader to avoid class caching.
5. **Fitness score**: passing/failing counts are converted to a numeric fitness value.

If compilation fails or a timeout occurs at any stage, the patch receives `-∞` fitness so it is unlikely to be selected. This makes compilation a hard constraint and naturally filters out invalid patches.

#### Why Reflection-Based Testing
JUnit Platform Launcher would require more setup and configuration, while reflection allows a small, reliable implementation for JUnit 5 test methods annotated with `@Test`. The trade-off is that more advanced JUnit features (parameterized tests, lifecycle methods) are not fully supported. This is listed as a limitation and a clear improvement target.

## Environment Setup
Requirements:
- Java 17+ (tested with OpenJDK 24.0.1)
- Gradle (wrapper included; wrapper downloads Gradle 9.0)
- No API keys required

Tested environment:
- OS: macOS (Darwin 25.2.0, ARM64)
- Java: OpenJDK 24.0.1 (Temurin)

Build:
```bash
./gradlew build
```

## How to Run
Single benchmark:
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 30" --no-daemon
```

Quick test (all benchmarks, short limits):
```bash
./test_quick.sh
```

Important CLI options:
- `--benchmark <path>` (required)
- `--seed <n>` (default: current time)
- `--maxGenerations <n>` (default: 50)
- `--timeLimitSec <n>` (default: 60)
- `--populationSize <n>` (default: 40)
- `--positiveTestWeight <w>` (default: 1.0)
- `--negativeTestWeight <w>` (default: 10.0)
- `--mutationWeight <w>` (default: 0.06)
- `--verbose` (prints extra information)

## Benchmarks
Each benchmark includes:
- `buggy/` and `fixed/` versions
- `tests/` (JUnit 5)
- `benchmark.json` (paths + test class names)
- `fault-localization.json` (line weights)

### B01_OffByOne
- Bug: off-by-one in loop condition (`<=` instead of `<`).
- Bug location (buggy): `Calculator.java:12`
- Expected fix: `for (int i = 0; i < array.length; i++)`
- Tests: `CalculatorTest` (3 test methods)

### B02_WrongOperator
- Bug: string equality uses `==` instead of `.equals()`.
- Bug location (buggy): `StringUtils.java:13`
- Expected fix: `return a.equals(b);`
- Tests: `StringUtilsTest` (2 test methods)

### B03_WrongCondition
- Bug: wrong comparison operator (`!=` instead of `==`).
- Bug location (buggy): `ArraySearch.java:4`
- Expected fix: `if (array[i] == value)`
- Tests: `ArraySearchTest` (2 test methods)

### B04_MissingStatement
- Bug: missing `return result;` in `power()`.
- Bug location (buggy): `MathUtils.java:18` (missing return before closing brace)
- Expected fix: insert `return result;`
- Tests: `MathUtilsTest` (2 test methods)

### B05_WrongVariable
- Bug: wrong comparison operator in `max()` (`<` instead of `>`).
- Bug location (buggy): `Statistics.java:21`
- Expected fix: `if (array[i] > max)`
- Tests: `StatisticsTest` (2 test methods)

## Creating New Benchmarks
1. Create a directory under `benchmarks/<NAME>/`.
2. Provide:
   - `buggy/<MainClass>.java`
   - `fixed/<MainClass>.java`
   - `tests/` with JUnit 5 test classes
   - `fault-localization.json` with weights (1.0 / 0.1 / 0.0)
   - `benchmark.json` with relative paths and test class names
3. Ensure:
   - Single-file Java program, default package (no `package` line)
   - At least one failing test on buggy version and passing on fixed version
   - All tests can compile and run with the provided Gradle setup
4. Run:
```bash
./gradlew run --args="--benchmark benchmarks/<NAME> --seed 42" --no-daemon
```

## Testing and Results (Executed)
Run date: **2026-01-31**  
Command: `./test_quick.sh`  
Parameters: `--seed 42 --maxGenerations 5 --timeLimitSec 15`

Summary:
| Benchmark | Best Fitness (Gen 0) | Result within 5 gens | Notes |
| --- | --- | --- | --- |
| B01_OffByOne | -19.00 (P=1, F=2) | Not solved | Stuck at same fitness across 5 generations |
| B02_WrongOperator | 2.00 (P=2, F=0) | Solved at Gen 0 | Tests already pass on buggy version (see notes below) |
| B03_WrongCondition | -20.00 (P=0, F=2) | Not solved | No progress within 5 generations |
| B04_MissingStatement | 2.00 (P=2, F=0) | Solved at Gen 0 | Patch inserts missing return |
| B05_WrongVariable | -9.00 (P=1, F=1) | Not solved | Stuck at same fitness across 5 generations |

### Patches Found (example runs)
**B04_MissingStatement (seed 42, maxGenerations 5):**
```
INSERT at line 18: return result;
```

**B02_WrongOperator (seed 42, maxGenerations 5):**  
All tests pass on the buggy version because string literals are interned, so `a == b` returns true for `"hello"` and `"hello"`. That means the tool can “succeed” without an actual fix. This benchmark needs a stronger failing test (see To-Do section).

## Answers to Section 2.1 Questions
1. **Most challenging parts**  
   - Implementing a reliable compile-and-test fitness loop with timeouts and isolation.  
   - Designing line-based edit operators that still preserve compilation.  
   - Balancing randomness (search diversity) with fault localization weights.

2. **Does the implementation run and behave as expected?**  
   - The tool runs end-to-end and produces patches, but behavior is stochastic.  
   - Some benchmarks do not converge within short limits (local optima).  
   - Compilation errors from discarded patches are printed unless output is suppressed (e.g., redirecting stderr).

3. **Did the tool fix any benchmarks? Which ones, with patches?**  
   - **B04_MissingStatement**: yes, inserts `return result;` at line 18.  
   - **B02_WrongOperator**: tests pass on the buggy version, so “success” does not guarantee a real fix. This benchmark needs a stronger test case.

4. **Benchmarks not fixed and reasons**  
   - **B01_OffByOne**, **B03_WrongCondition**, **B05_WrongVariable** were not fixed within 5 generations.  
   - Likely causes: limited search budget, line-based edits, weak donor selection, and lack of targeted operator mutations.

5. **Limitations / simplifying assumptions**  
   - Single-file Java programs only, default package.  
   - Line-based editing (no AST).  
   - Simplified test execution (reflection-based; limited JUnit features).  
   - No patch minimization.  
   - Fault localization weights are provided manually in JSON.

6. **How was the tool tested?**  
   - `./test_quick.sh` runs all benchmarks with short limits and a fixed seed.  
   - Individual benchmarks were also run with verbose output to inspect patches.

## What Still Needs To Be Done (for the maximum grade)
- Strengthen **B02_WrongOperator** tests: add a case with `new String("hello")` so the bug is actually revealed.
- Increase search effectiveness: raise `maxGenerations` / `timeLimitSec` and/or add adaptive mutation or restart strategies.
- Improve donor selection for INSERT/REPLACE: prefer statements from the same method or similar context, exclude empty/whitespace lines.
- Add patch validation/minimization so redundant or no-op edits are not saved.
- Automate fault localization (e.g., via coverage) instead of maintaining weights manually.
- Use JUnit Platform Launcher for more complete test support (setup/teardown, parameterized tests).
- Cache compilation/test results to speed up fitness evaluation.
- Demonstrate successful fixes on at least 2–3 benchmarks and record them in the report.
