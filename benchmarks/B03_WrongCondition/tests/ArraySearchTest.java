import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArraySearchTest {
    @Test
    public void testFindIndex() {
        int[] arr = {1, 2, 3, 4, 5};
        assertEquals(2, ArraySearch.findIndex(arr, 3));
        assertEquals(0, ArraySearch.findIndex(arr, 1));
        assertEquals(4, ArraySearch.findIndex(arr, 5));
        assertEquals(-1, ArraySearch.findIndex(arr, 10));
    }

    @Test
    public void testContains() {
        int[] arr = {10, 20, 30};
        assertTrue(ArraySearch.contains(arr, 20));
        assertFalse(ArraySearch.contains(arr, 50));
    }
}

