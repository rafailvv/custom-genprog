import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CalculatorTest {

    @Test
    public void testMax_3_and_5_returns_5() {
        assertEquals(5, Calculator.max(3, 5));
    }

    @Test
    public void testMax_10_and_5_returns_10() {
        assertEquals(10, Calculator.max(10, 5));
    }

    @Test
    public void testMax_0_and_0_returns_0() {
        assertEquals(0, Calculator.max(0, 0));
    }

    @Test
    public void testSum_1_2_3_returns_6() {
        int[] arr1 = {1, 2, 3};
        assertEquals(6, Calculator.sum(arr1));
    }

    @Test
    public void testSum_of_zeros() {
        int[] arr1 = {0, 0, 0};
        assertEquals(0, Calculator.sum(arr1));
    }

    @Test
    public void testSum_of_ones() {
        int[] arr1 = {1, 1, 1, 1, 1};
        assertEquals(arr1.length, Calculator.sum(arr1));
    }

    @Test
    public void testSum_10_20_30_40_returns_100() {
        int[] arr2 = {10, 20, 30, 40};
        assertEquals(100, Calculator.sum(arr2));
    }

    @Test
    public void testSum_single_element() {
        int[] arr3 = {5};
        assertEquals(5, Calculator.sum(arr3));
    }

    @Test
    public void testSumEmpty() {
        int[] empty = {};
        assertEquals(0, Calculator.sum(empty));
    }
}
