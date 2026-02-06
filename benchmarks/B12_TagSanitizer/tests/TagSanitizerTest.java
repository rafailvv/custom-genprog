import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagSanitizerTest {

    @Test
    public void sanitizeTags_trimsAndLowercases() {
        List<String> tags = TagSanitizer.sanitizeTags(Arrays.asList("  API ", "Ops", "Data"));
        assertEquals(List.of("api", "ops", "data"), tags);
    }

    @Test
    public void sanitizeTags_skipsNullAndBlankEntries() {
        List<String> tags = TagSanitizer.sanitizeTags(Arrays.asList("backend", null, " ", "qa"));
        assertEquals(List.of("backend", "qa"), tags);
    }

    @Test
    public void sanitizeTags_truncatesVeryLongTags() {
        List<String> tags = TagSanitizer.sanitizeTags(List.of("this-is-a-very-long-tag-name"));
        assertEquals(List.of("this-is-a-very-long-"), tags);
    }

    @Test
    public void uniquePreserveOrder_deduplicatesAfterNormalization() {
        List<String> tags = TagSanitizer.uniquePreserveOrder(Arrays.asList(" API", "api", "Ops", "ops "));
        assertEquals(List.of("api", "ops"), tags);
    }

    @Test
    public void toCsv_joinsUniqueTags() {
        assertEquals("backend,qa", TagSanitizer.toCsv(Arrays.asList("backend", "backend", "qa")));
    }
}
