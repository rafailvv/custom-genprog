import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MathUtilsTest {

    @Test
    public void sum_someNumbers() {
        int result = MathUtils.sum(1, 2, 3, 4);
        assertEquals(10, result);
    }

    @Test
    public void sum_noNumbers() {
        int result = MathUtils.sum();
        assertEquals(0, result);
    }

    @Test
    public void sum_negativeNumbers() {
        int result = MathUtils.sum(-1, -2, -3);
        assertEquals(-6, result);
    }

    @Test
    public void multiply_someNumbers() {
        int result = MathUtils.multiply(2, 3, 4);
        assertEquals(24, result);
    }

    @Test
    public void multiply_noNumbers() {
        int result = MathUtils.multiply();
        assertEquals(1, result);
    }

    @Test
    public void multiply_withZero() {
        int result = MathUtils.multiply(2, 0, 4);
        assertEquals(0, result);
    }
}
