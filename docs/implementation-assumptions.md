# Implementation Assumptions and Limitations

This document captures the current assumptions, simplifications, and known limits of the APR implementation in this repository.

## 1. Scope and Input Model

### 1.1 Program scope
- Benchmarks are expected to be single-target repairs: one buggy Java source file plus tests.
- The repaired class is compiled from patched source each evaluation.
- Tests are discovered from benchmark test sources and executed against the compiled candidate class.

### 1.2 Benchmark format assumptions
- Each benchmark provides `benchmark.json` and `fault-localization.json`.
- `fault-localization.json` uses per-line suspiciousness values.
- Current assignment-oriented convention is `1.0`, `0.1`, `0.0`; the loader does not re-normalize weights.

### 1.3 Java/test assumptions
- JavaParser-compatible Java syntax is required for the repaired source.
- Test discovery assumes JUnit-style methods annotated with `@Test`.
- Package-heavy or multi-module layouts are outside the intended benchmark scope.

## 2. Patch Representation and Operators

### 2.1 Representation
- A patch is an ordered edit script over AST statements/expressions (not raw text lines).
- Mutable statement list excludes `BlockStmt` nodes.
- Hard limit: at most 3 applied edits per patch (`MAX_EDITS_PER_PATCH = 3`).

### 2.2 Implemented edit operators
- GenProg core operators: `DELETE`, `INSERT`, `SWAP`.
- Additional expression-level operators: `REPLACE_EXPR`, `MUTATE_BINARY_OPERATOR`, `NEGATE_EXPRESSION`.

### 2.3 Safety constraints
- Edit application is guarded by AST compatibility checks and structural validity checks.
- `SWAP` forbids ancestor/descendant swaps.
- Expression replacement is constrained by replaceable-expression predicates and compatibility filtering.
- Invalid AST rewrite attempts are rejected and do not crash the search loop.

## 3. Fault Localization Usage (Strict Targeting)

### 3.1 Statement target selection
- Mutation targets must have suspiciousness `> 0.0`.
- Statements mapped to `0.0` are not selected as mutation targets.
- Suspiciousness is read directly from mapped source lines; no normalization step is applied.

### 3.2 Donor selection
- Donor candidates can come from broader statement pools (including statements with suspiciousness `0.0`, subject to operator-specific constraints).
- For expression replacement, donor ranking favors same enclosing callable and same statement kind.

## 4. Evolutionary Search

### 4.1 Population and loop
- Default population size: `40`.
- Defaults: `maxGenerations = 50`, `timeLimitSec = 60`, `mutationWeight = 0.06`.
- The GA uses initialization, evaluation, selection, crossover, mutation, and elitism.

### 4.2 Selection and crossover
- Selection uses tournament selection (`k = 3`) on viable candidates.
- Crossover rate is fixed at `0.5`.
- Crossover is one-point over normalized, source-indexed edit scripts, then rebased and replayed on fresh patches.

### 4.3 Mutation policy
- Mutation follows a GenProg-style weighted loop:
  - Iterate candidate statements.
  - Apply mutation only when both checks pass:
    - Bernoulli(`mutationWeight`)
    - Bernoulli(`statementSuspiciousness`)
- Operator choice is biased toward GenProg core edits with a smaller extension window for expression edits.

## 5. Fitness Evaluation

### 5.1 Evaluation pipeline
- For each candidate:
  - compile patched source;
  - run test suite with per-test timeout;
  - compute fitness from test outcomes.
- Compile/test runs are isolated through temporary class output and class loading.

### 5.2 Fitness score used in code
- The evaluator partitions tests using baseline buggy behavior:
  - positive group: tests that pass on buggy;
  - negative group: tests that fail on buggy.
- Score is computed as:
  - `fitness = positiveWeight * passedPositive + negativeWeight * passedNegative`
- Default weights: `positiveWeight = 1.0`, `negativeWeight = 10.0`.
- Note: generation logs print global `Passing/Failing` over all executed tests, while the fitness value above is computed from positive/negative partitions.

### 5.3 Timeouts and failures
- Candidate evaluation timeout: `30s`.
- Compilation timeout: `5s`.
- Per-test timeout: `2s`.
- Compile/evaluation failures produce a non-viable result (fitness `0.0`, `compiles=false`) and are treated as unsuccessful candidates.

## 6. Output and Execution Modes

### 6.1 APR run mode
- The tool reports generation progress and final result.
- On success, patched source is saved under `out/<benchmark>/patch_<timestamp>/`.
- On failure, the best-so-far patch can still be materialized for inspection.

### 6.2 Test-only mode
- CLI option `--runTests buggy|fixed` executes compile+test without GA search.
- This mode is intended for benchmark validation and debugging.

## 7. Known Limitations

1. No patch minimization stage after finding a plausible patch.
2. Hard patch-size cap (`<= 3` edits) can block fixes requiring longer scripts.
3. Simplified test execution path (reflection-based) may not cover all advanced JUnit features.
4. Designed for assignment-style benchmark structure, not general industrial multi-module projects.
5. Search remains stochastic; identical seeds improve reproducibility but do not guarantee bit-identical timing behavior across environments.
