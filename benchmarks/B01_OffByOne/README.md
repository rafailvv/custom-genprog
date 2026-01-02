# B01_OffByOne

## Bug Description
The `sum` method has an off-by-one error in the loop condition. It uses `i <= array.length` instead of `i < array.length`, causing an ArrayIndexOutOfBoundsException.

## Bug Location
Line 9: `for (int i = 0; i <= array.length; i++)`

## Expected Fix
Change line 9 to: `for (int i = 0; i < array.length; i++)`

## Running Tests
```bash
cd benchmarks/B01_OffByOne
javac -cp ".:../../build/libs/*" buggy/Calculator.java tests/CalculatorTest.java
java -cp ".:../../build/libs/*" org.junit.platform.console.ConsoleLauncher --class-path . --select-class CalculatorTest
```

