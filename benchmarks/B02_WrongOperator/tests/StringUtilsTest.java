import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {

    @Test
    public void testIsEmptyForEmptyString() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    public void testIsEmptyForNull() {
        assertTrue(StringUtils.isEmpty(null));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(StringUtils.isEmpty("hello"));
    }

    @Test
    public void testEqualsSimple() {
        assertTrue(StringUtils.equals("hello", "hello"));
    }

    @Test
    public void testEqualsDifferentObjectsSameValue() {
        assertTrue(StringUtils.equals("hello", new String("hello")));
    }

    @Test
    public void testNotEquals() {
        assertFalse(StringUtils.equals("hello", "world"));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(StringUtils.equals(null, null));
        assertFalse(StringUtils.equals("hello", null));
        assertFalse(StringUtils.equals(null, "hello"));
    }
}

