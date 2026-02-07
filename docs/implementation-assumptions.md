# Implementation Assumptions and Limitations

This document describes the assumptions, simplifications, and limitations of the APR tool implementation.

## Supported Program Types

### Single-File Programs
- The tool only supports single-file Java programs (one `.java` file per benchmark)
- No package declarations are required (programs can be in the default package)
- Programs should be self-contained (no external dependencies beyond standard library)

### Java Language Features
- Basic Java syntax (classes, methods, variables, control flow)
- Arrays and basic data structures
- Standard library classes (String, Integer, etc.)

### Not Supported
- Multi-file programs (multiple classes in separate files)
- Package declarations (simplification)
- Complex inheritance hierarchies
- Generics (may work but not tested)
- Lambda expressions (may work but not tested)
- Annotations (beyond @Test for JUnit)

## Edit Operations

### Edit Types
1. **DELETE**: Removes a line from the source code
2. **INSERT**: Inserts a donor statement (from another part of the file) at a specific line
3. **REPLACE**: Replaces a line with a donor statement

### Patch Size Limitation
- Maximum 3 edits per patch (configurable)
- This limitation helps ensure patches compile and reduces search space

## Fault Localization

### Weight Scheme
The tool uses a simplified fault localization scheme with three weight levels:
- **Weight 1.0**: Statements covered only by failing tests
- **Weight 0.1**: Statements covered by both failing and passing tests
- **Weight 0.0**: All other statements

### Input Format
- Fault localization weights are provided in a JSON file (`fault-localization.json`)
- The file contains an array of objects with `lineNumber` and `weight` fields
- Weights are used for probabilistic selection of edit locations

### Not Perfect Localization
- The tool does NOT use perfect fault localization
- Some statements with weight 0.1 or 0.0 may be selected for edits
- This makes the search more realistic and less "cheating"

## Genetic Algorithm

### Population
- Default population size: 40
- Initial population: Random patches generated using genetic operators

### Selection
- Tournament selection with tournament size 3
- Alternative: Stochastic Universal Sampling (not implemented, but could be added)

### Variation Operators
- **Mutation**: Applied with probability based on `mutationWeight` (default: 0.06)
- **Crossover**: Applied with probability 0.5 (fixed)
- Mutation can add or remove edits from a patch

### Fitness Function
- Formula: `fitness = WPosT * passingTests - WNegT * failingTests`
- Default weights:
  - WPosT (positive test weight): 1.0
  - WNegT (negative test weight): 10.0
- Higher fitness is better
- Patches that don't compile get fitness = -∞

### Termination Conditions
1. **Success**: Found a patch where all tests pass
2. **Generation limit**: Reached maximum number of generations (default: 50)
3. **Time limit**: Exceeded time limit (default: 60 seconds)

## Compilation and Testing

### Compilation
- Uses JavaCompiler API (`javax.tools.JavaCompiler`)
- Compiles modified source code in a temporary directory
- If compilation fails, the patch is rejected (fitness = -∞)

### Test Execution
- Uses reflection to discover and run JUnit test methods
- Tests are identified by `@Test` annotation
- Test results are collected (passing/failing counts)
- Each test execution uses a fresh classloader

### Limitations
- Test execution is simplified (doesn't use full JUnit Platform Launcher)
- May not support all JUnit features (parameterized tests, etc.)
- Test setup/teardown methods are not explicitly handled

## Patch Minimization

**Not Implemented**: The tool does NOT include a patch minimization step.
- Once a patch is found that passes all tests, it is returned as-is
- No attempt is made to reduce the number of edits
- This is a documented simplification

## Output

### Success Case
- Prints the patch (list of edits) to console
- Saves patched source file to `out/<benchmark>/patch_<timestamp>/`
- Reports generation count and execution time

### Failure Case
- Reports best fitness found
- Reports number of generations executed
- No patch file is saved

## Reproducibility

- Use `--seed` parameter to set random seed
- Same seed + same benchmark = same search behavior (in theory)
- In practice, timing and system differences may cause slight variations

## Performance Considerations

- Each fitness evaluation requires compilation and test execution
- This is expensive, so the tool may be slow for large programs
- No caching of compilation/test results (could be added as optimization)
- Temporary files are cleaned up after evaluation

## Known Limitations

1. **Line-based editing**: Less precise than AST-based approaches
2. **No patch minimization**: Patches may contain unnecessary edits
3. **Single-file only**: Cannot handle multi-file programs
4. **Simplified test execution**: May not support all JUnit features
5. **No dependency management**: Programs must be self-contained
6. **Limited Java features**: Complex language features may not work

## Future Improvements

Potential enhancements (not implemented):
- AST-based editing for more precise patches
- Patch minimization step
- Multi-file program support
- Better test execution using JUnit Platform Launcher
- Caching of compilation/test results
- Support for more Java language features

