import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilsTest {

    // Tests for findIndexOf
    @Test
    void findIndexOf_emptyArray_returnsMinusOne() {
        assertEquals(-1, ArrayUtils.findIndexOf(5));
    }

    @Test
    void findIndexOf_firstElement_returnsZero() {
        assertEquals(0, ArrayUtils.findIndexOf(10, 10, 20, 30));
    }

    @Test
    void findIndexOf_middleElement_returnsIndex() {
        assertEquals(2, ArrayUtils.findIndexOf(30, 5, 10, 30, 40));
    }

    @Test
    void findIndexOf_notFound_returnsMinusOne() {
        assertEquals(-1, ArrayUtils.findIndexOf(99, 1, 2, 3, 4));
    }

    // Tests for countOccurrences (designed to reveal the bug where index is incremented twice)
    @Test
    void countOccurrences_emptyArray_returnsZero() {
        assertEquals(0, ArrayUtils.countOccurrences(1));
    }

    @Test
    void countOccurrences_singleOccurrenceAtOddIndex_returnsOne() {
        // 7 is at index 1; buggy implementation skips odd indices and will return 0
        assertEquals(1, ArrayUtils.countOccurrences(7, 3, 7, 9));
    }

    @Test
    void countOccurrences_consecutiveOccurrences_returnsCorrectCount() {
        // three consecutive 1s in the middle
        assertEquals(3, ArrayUtils.countOccurrences(1, 2, 1, 1, 1, 2));
    }

    @Test
    void countOccurrences_allElementsMatch_returnsLength() {
        assertEquals(4, ArrayUtils.countOccurrences(5, 5, 5, 5, 5));
    }
}