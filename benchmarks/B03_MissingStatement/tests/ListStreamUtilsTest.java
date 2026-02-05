import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListStreamUtilsTest {

    // Tests for joinObjectsWithComma
    @Test
    public void joinObjectsWithComma_empty_returnsEmptyString() {
        assertEquals("", ListStreamUtils.joinObjectsWithComma());
    }

    @Test
    public void joinObjectsWithComma_singleElement_returnsElementToString() {
        assertEquals("hello", ListStreamUtils.joinObjectsWithComma("hello"));
    }

    @Test
    public void joinObjectsWithComma_multipleElements_includesCommas() {
        // This should reveal the bug: expected commas between elements
        assertEquals("a, b, c", ListStreamUtils.joinObjectsWithComma("a", "b", "c"));
    }

    @Test
    public void joinObjectsWithComma_handlesNullElements() {
        assertEquals("a, null, c", ListStreamUtils.joinObjectsWithComma("a", null, "c"));
    }

    // Tests for joinObjectsTwiceWithComma
    @Test
    public void joinObjectsTwiceWithComma_empty_returnsEmptyString() {
        assertEquals("", ListStreamUtils.joinObjectsTwiceWithComma());
    }

    @Test
    public void joinObjectsTwiceWithComma_singleElement_repeatsWithComma() {
        assertEquals("x, x", ListStreamUtils.joinObjectsTwiceWithComma("x"));
    }

    @Test
    public void joinObjectsTwiceWithComma_multipleElements_repeatsSequence() {
        assertEquals("a, b, a, b", ListStreamUtils.joinObjectsTwiceWithComma("a", "b"));
    }

    @Test
    public void joinObjectsTwiceWithComma_mixedTypes_toStringApplied() {
        assertEquals("1, two, 3.0, 1, two, 3.0", ListStreamUtils.joinObjectsTwiceWithComma(1, "two", 3.0));
    }
}