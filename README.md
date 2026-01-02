# Automated Program Repair Tool

A Java implementation of the GenProg algorithm for automatically fixing bugs in Java programs.

**Group 7**  
Authors: Tim Greller, Jonas Heitz, Rafail Venediktov

## Overview

This tool implements a genetic programming approach to automated program repair. Given a buggy Java program and a set of test cases, it attempts to find a patch that makes all tests pass.

## Features

- Genetic algorithm-based search for patches
- Line-based edit operations (delete, insert, replace)
- Fault localization with weighted statement selection
- Automatic compilation and test execution
- Configurable parameters (population size, weights, etc.)

## Quick Start

### Build the Project

```bash
./gradlew build
```

### Run the Tool

**Simple run (clean output, suppresses compilation errors):**
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null
```

**With all output visible:**
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 30" --no-daemon
```

**With custom parameters:**
```bash
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 123 --maxGenerations 100 --timeLimitSec 120 --populationSize 60" --no-daemon 2>/dev/null
```

> **Note:** Adding `2>/dev/null` suppresses compilation error messages from intermediate patches, making the output cleaner and easier to read. The main results and progress are still displayed.

### Command Line Options

- `--benchmark <path>` (required): Path to benchmark directory
- `--seed <number>`: Random seed for reproducibility (default: current time)
- `--maxGenerations <n>`: Maximum number of generations (default: 50)
- `--timeLimitSec <n>`: Time limit in seconds (default: 60)
- `--populationSize <n>`: Population size (default: 40)
- `--positiveTestWeight <w>`: Weight for passing tests (default: 1.0)
- `--negativeTestWeight <w>`: Weight for failing tests (default: 10.0)
- `--mutationWeight <w>`: Mutation weight (default: 0.06)
- `--verbose`: Enable verbose output

## Benchmarks

The project includes 5 benchmarks located in the `benchmarks/` directory:

1. **B01_OffByOne**: Off-by-one error in loop condition
2. **B02_WrongOperator**: Wrong operator (== instead of .equals())
3. **B03_WrongCondition**: Wrong condition in if statement
4. **B04_MissingStatement**: Missing return statement
5. **B05_WrongVariable**: Wrong comparison operator

Each benchmark contains:
- `buggy/`: Buggy version of the program
- `fixed/`: Correct version (for reference)
- `tests/`: JUnit test cases
- `fault-localization.json`: Statement weights for fault localization
- `benchmark.json`: Benchmark configuration
- `README.md`: Benchmark-specific documentation

## Testing All Benchmarks

You can test all benchmarks at once using the provided script:

```bash
./test_quick.sh
```

Or test them individually with the APR tool:

```bash
# B01_OffByOne
./gradlew run --args="--benchmark benchmarks/B01_OffByOne --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null

# B02_WrongOperator
./gradlew run --args="--benchmark benchmarks/B02_WrongOperator --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null

# B03_WrongCondition
./gradlew run --args="--benchmark benchmarks/B03_WrongCondition --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null

# B04_MissingStatement
./gradlew run --args="--benchmark benchmarks/B04_MissingStatement --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null

# B05_WrongVariable
./gradlew run --args="--benchmark benchmarks/B05_WrongVariable --seed 42 --maxGenerations 30" --no-daemon 2>/dev/null
```

## Running Benchmarks Manually

To run tests for a specific benchmark manually (without the APR tool):

```bash
cd benchmarks/B01_OffByOne
javac -cp ".:../../build/libs/*" buggy/Calculator.java tests/CalculatorTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class CalculatorTest
```

## Expected Output

**When running the tool, you'll see progress for each generation:**
```
Starting genetic algorithm...
Generation 0: Best fitness = -19,00 (Passing: 1, Failing: 2)
Generation 1: Best fitness = -19,00 (Passing: 1, Failing: 2)
Generation 2: Best fitness = -9,00 (Passing: 2, Failing: 1)
...
```

**When the tool finds a patch:**
```
=== Results ===
Generations: 15
Time: 12.5 seconds
SUCCESS: Found a patch that passes all tests!

Patch:
REPLACE at line 12: for (int i = 0; i < array.length; i++)

Patched file saved to: out/B01_OffByOne/patch_20240101_120000/Calculator.java
```

**If no patch is found within limits:**
```
=== Results ===
Generations: 30
Time: 8.5 seconds
No solution found within limits.
Best fitness: Fitness: -19,00 (Passing: 1/3, Failing: 2, Compiles: true, AllPass: false)
```

> **Note:** GenProg is a stochastic algorithm, so it may not find a patch every time. Try different seeds or increase the number of generations for better results.

## Troubleshooting

1. **Compilation errors**: Make sure Java 17+ is installed
2. **Gradle not found**: Install Gradle or use the wrapper (`gradle wrapper` first)
3. **Tests not running**: Check that JUnit dependencies are in `build/libs/`
4. **Benchmark not found**: Verify the path is correct and `benchmark.json` exists

## Project Structure

```
.
├── src/main/java/edu/passau/apr/
│   ├── Main.java                    # Entry point
│   ├── algorithm/                   # Genetic algorithm implementation
│   ├── config/                      # Configuration classes
│   ├── evaluator/                   # Fitness evaluation
│   ├── model/                       # Data models
│   ├── operator/                    # Genetic operators
│   ├── selection/                   # Selection operators
│   └── util/                        # Utility classes
├── benchmarks/                      # Benchmark programs
├── docs/                            # Documentation
└── build.gradle                     # Build configuration
```

## Implementation Assumptions

See `docs/implementation-assumptions.md` for detailed information about:
- Supported program types
- Edit operations
- Fault localization approach
- Limitations

## Requirements

- Java 17 or higher
- Gradle 7.0 or higher (or use Gradle Wrapper)

## License

This project is part of a university assignment.

