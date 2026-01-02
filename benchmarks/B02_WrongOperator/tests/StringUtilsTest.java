import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StringUtilsTest {
    @Test
    public void testIsEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("hello"));
    }

    @Test
    public void testEquals() {
        assertTrue(StringUtils.equals("hello", "hello"));
        assertFalse(StringUtils.equals("hello", "world"));
        assertTrue(StringUtils.equals(null, null));
        assertFalse(StringUtils.equals("hello", null));
        assertFalse(StringUtils.equals(null, "hello"));
    }
}

