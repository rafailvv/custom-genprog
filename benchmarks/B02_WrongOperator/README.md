# B02_WrongOperator

## Bug Description
The `equals` method uses reference equality (`==`) instead of value equality (`.equals()`), causing it to return false for equal strings.

## Bug Location
Line 9: `return a == b;`

## Expected Fix
Change line 9 to: `return a.equals(b);`

## Running Tests
```bash
cd benchmarks/B02_WrongOperator
javac -cp ".:../../build/libs/*" StringUtils.java tests/StringUtilsTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class StringUtilsTest
```

