import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MathUtilsTest {
    @Test
    public void testFactorial() {
        assertEquals(1, MathUtils.factorial(0));
        assertEquals(1, MathUtils.factorial(1));
        assertEquals(6, MathUtils.factorial(3));
        assertEquals(24, MathUtils.factorial(4));
    }

    @Test
    public void testPower_positiveExponent() {
        assertEquals(8, MathUtils.power(2, 3));
    }

    @Test
    public void testPower_zeroExponent() {
        assertEquals(1, MathUtils.power(5, 0));
    }

    @Test
    public void testPower_square() {
        assertEquals(25, MathUtils.power(5, 2));
    }
}

