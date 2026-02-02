import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatisticsTest {
    public class StatisticsTest {
        @Test
        public void testMin() {
            int[] arr = {5, 2, 8, 1, 9};
            assertEquals(1, Statistics.min(arr));
            assertEquals(5, Statistics.min(new int[]{5}));
        }

        @Test
        public void testMaxMixed() {
            int[] arr = {5, 2, 8, 1, 9};
            assertEquals(9, Statistics.max(arr));
        }

        @Test
        public void testMaxSingle() {
            assertEquals(5, Statistics.max(new int[]{5}));
        }

        @Test
        public void testMaxAllNegative() {
            int[] arr = {-5, -2, -8, -1, -9};
            assertEquals(-1, Statistics.max(arr));
        }

        @Test
        public void testMaxDuplicates() {
            int[] arr = {3, 3, 3};
            assertEquals(3, Statistics.max(arr));
        }

        @Test
        public void testMaxDescending() {
            int[] arr = {9, 7, 5, 3, 1};
            assertEquals(9, Statistics.max(arr));
        }

        @Test
        public void testMaxEmpty() {
            assertEquals(Integer.MIN_VALUE, Statistics.max(new int[]{}));
        }
    }
}

