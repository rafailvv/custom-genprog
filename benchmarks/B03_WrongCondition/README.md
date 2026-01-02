# B03_WrongCondition

## Bug Description
The `findIndex` method uses the wrong comparison operator (`!=` instead of `==`), causing it to return the first index where the value is NOT equal.

## Bug Location
Line 4: `if (array[i] != value)`

## Expected Fix
Change line 4 to: `if (array[i] == value)`

## Running Tests
```bash
cd benchmarks/B03_WrongCondition
javac -cp ".:../../build/libs/*" ArraySearch.java tests/ArraySearchTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class ArraySearchTest
```

