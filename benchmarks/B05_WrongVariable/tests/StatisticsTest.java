import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatisticsTest {
    @Test
    public void testMin() {
        int[] arr = {5, 2, 8, 1, 9};
        assertEquals(1, Statistics.min(arr));
        assertEquals(5, Statistics.min(new int[]{5}));
    }

    @Test
    public void testMax() {
        int[] arr = {5, 2, 8, 1, 9};
        assertEquals(9, Statistics.max(arr));
        assertEquals(5, Statistics.max(new int[]{5}));
    }
}

