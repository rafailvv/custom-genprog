import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TagSanitizer {

    public static List<String> sanitizeTags(List<String> rawTags) {
        List<String> cleaned = new ArrayList<>();
        if (rawTags == null) {
            return cleaned;
        }

        for (String tag : rawTags) {
            if (tag == null) {
                continue;
            }
            if (tag.trim().isEmpty()) {
                continue;
            }

            String normalized = tag.trim().toLowerCase();
            if (normalized.length() < 20) {
                normalized = normalized.substring(0, Math.min(20, normalized.length()));
            }

            cleaned.add(normalized);
        }

        return cleaned;
    }

    public static List<String> uniquePreserveOrder(List<String> tags) {
        return new ArrayList<>(new LinkedHashSet<>(sanitizeTags(tags)));
    }

    public static String toCsv(List<String> tags) {
        return String.join(",", uniquePreserveOrder(tags));
    }
}
