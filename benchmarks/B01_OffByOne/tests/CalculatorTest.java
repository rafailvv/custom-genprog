import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {
    @Test
    public void testMax() {
        assertEquals(5, Calculator.max(3, 5));
        assertEquals(10, Calculator.max(10, 5));
        assertEquals(0, Calculator.max(0, 0));
    }

    @Test
    public void testSum() {
        int[] arr1 = {1, 2, 3};
        assertEquals(6, Calculator.sum(arr1));
        
        int[] arr2 = {10, 20, 30, 40};
        assertEquals(100, Calculator.sum(arr2));
        
        int[] arr3 = {5};
        assertEquals(5, Calculator.sum(arr3));
    }

    @Test
    public void testSumEmpty() {
        int[] empty = {};
        assertEquals(0, Calculator.sum(empty));
    }
}

