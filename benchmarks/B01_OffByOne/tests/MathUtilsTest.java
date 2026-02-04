import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {

    @Test
    void sum_someNumbers() {
        int result = MathUtils.sum(1, 2, 3, 4);
        assertEquals(10, result);
    }

    @Test
    void sum_noNumbers() {
        int result = MathUtils.sum();
        assertEquals(0, result);
    }

    @Test
    void sum_negativeNumbers() {
        int result = MathUtils.sum(-1, -2, -3);
        assertEquals(-6, result);
    }

    @Test
    void multiply_someNumbers() {
        int result = MathUtils.multiply(2, 3, 4);
        assertEquals(24, result);
    }

    @Test
    void multiply_noNumbers() {
        int result = MathUtils.multiply();
        assertEquals(1, result);
    }

    @Test
    void multiply_withZero() {
        int result = MathUtils.multiply(2, 0, 4);
        assertEquals(0, result);
    }
}