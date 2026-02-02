import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArraySearchTest {

    @Test
    public void testFindIndexMiddle() {
        int[] arr = {1, 2, 3, 4, 5};
        assertEquals(2, ArraySearch.findIndex(arr, 3));
    }

    @Test
    public void testFindIndexFirst() {
        int[] arr = {1, 2, 3, 4, 5};
        assertEquals(0, ArraySearch.findIndex(arr, 1));
    }

    @Test
    public void testFindIndexLast() {
        int[] arr = {1, 2, 3, 4, 5};
        assertEquals(4, ArraySearch.findIndex(arr, 5));
    }

    @Test
    public void testFindIndexNotFound() {
        int[] arr = {1, 2, 3, 4, 5};
        assertEquals(-1, ArraySearch.findIndex(arr, 10));
    }

    @Test
    public void testEmptyArray() {
        int[] arr = {};
        assertEquals(-1, ArraySearch.findIndex(arr, 0));
    }

    @Test
    public void testOnlyContainsCorrect() {
        int[] arr = {1};
        assertEquals(0, ArraySearch.findIndex(arr, 1));
    }


    @Test
    public void testContains() {
        int[] arr = {10, 20, 30};
        assertTrue(ArraySearch.contains(arr, 20));
        assertFalse(ArraySearch.contains(arr, 50));
    }
}

