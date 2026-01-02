# B05_WrongVariable

## Bug Description
The `max` method uses the wrong comparison operator (`<` instead of `>`), causing it to find the minimum instead of maximum.

## Bug Location
Line 18: `if (array[i] < max)`

## Expected Fix
Change line 18 to: `if (array[i] > max)`

## Running Tests
```bash
cd benchmarks/B05_WrongVariable
javac -cp ".:../../build/libs/*" Statistics.java tests/StatisticsTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class StatisticsTest
```

