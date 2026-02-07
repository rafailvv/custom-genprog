import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class StreamUtilsTest {

    // Tests for nonNullStringJoin (should filter out nulls and not append "bug")
    @Test
    public void nonNullStringJoin_emptyList_returnsEmptyString() {
        assertEquals("", StreamUtils.nonNullStringJoin(List.of()));
    }

    @Test
    public void nonNullStringJoin_allNulls_returnsEmptyString() {
        assertEquals("", StreamUtils.nonNullStringJoin(Arrays.asList(null, null)));
    }

    @Test
    public void nonNullStringJoin_mixedSkipsNulls_andNoBugSuffix() {
        assertEquals("1, a", StreamUtils.nonNullStringJoin(Arrays.asList(1, null, "a")));
    }

    @Test
    public void nonNullStringJoin_allNonNull_toStringOnly() {
        assertEquals("x, y", StreamUtils.nonNullStringJoin(List.of("x", "y")));
    }

    // Tests for duplicateAndJoin
    @Test
    public void duplicateAndJoin_emptyList_returnsEmptyString() {
        assertEquals("", StreamUtils.duplicateAndJoin(List.of()));
    }

    @Test
    public void duplicateAndJoin_singleElement_repeatsWithComma() {
        assertEquals("z, z", StreamUtils.duplicateAndJoin(List.of("z")));
    }

    @Test
    public void duplicateAndJoin_multipleElements_repeatsSequence() {
        assertEquals("a, a, b, b", StreamUtils.duplicateAndJoin(List.of("a", "b")));
    }

    @Test
    public void duplicateAndJoin_mixedTypes_toStringApplied() {
        assertEquals("1, 1, two, two, 3.0, 3.0", StreamUtils.duplicateAndJoin(List.of(1, "two", 3.0)));
    }
}