import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayIndexUtilTest {

    // Tests for getFirstElement
    @Test
    void getFirstElement_singleElement_returnsThatElement() {
        assertEquals(5, ArrayIndexUtil.getFirstElement(new int[]{5}));
    }

    @Test
    void getFirstElement_multipleElements_returnsFirst() {
        assertEquals(10, ArrayIndexUtil.getFirstElement(new int[]{10, 20, 30}));
    }

    @Test
    void getFirstElement_negativeFirst_returnsNegative() {
        assertEquals(-1, ArrayIndexUtil.getFirstElement(new int[]{-1, 2, 3}));
    }

    @Test
    void getFirstElement_boundaryMinValue_returnsMin() {
        assertEquals(Integer.MIN_VALUE, ArrayIndexUtil.getFirstElement(new int[]{Integer.MIN_VALUE, 0, 1}));
    }

    // Tests for getLastElement
    @Test
    void getLastElement_singleElement_returnsThatElement() {
        assertEquals(7, ArrayIndexUtil.getLastElement(new int[]{7}));
    }

    @Test
    void getLastElement_multipleElements_returnsLast() {
        assertEquals(30, ArrayIndexUtil.getLastElement(new int[]{10, 20, 30}));
    }

    @Test
    void getLastElement_negativeLast_returnsNegative() {
        assertEquals(-2, ArrayIndexUtil.getLastElement(new int[]{1, -2}));
    }

    @Test
    void getLastElement_boundaryMaxValue_returnsMax() {
        assertEquals(Integer.MAX_VALUE, ArrayIndexUtil.getLastElement(new int[]{0, 1, Integer.MAX_VALUE}));
    }
}