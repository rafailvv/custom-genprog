# B04_MissingStatement

## Bug Description
The `power` method is missing a return statement, causing a compilation error.

## Bug Location
Line 14: Missing `return result;` statement

## Expected Fix
Add `return result;` after line 13 (before the closing brace).

## Running Tests
```bash
cd benchmarks/B04_MissingStatement
javac -cp ".:../../build/libs/*" MathUtils.java tests/MathUtilsTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class MathUtilsTest
```

