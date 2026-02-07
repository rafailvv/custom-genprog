import java.util.List;
import java.util.stream.Collectors;

public class StreamUtils {
    public static String nonNullStringJoin(List<Object> objects) {
        return objects.stream()
                .filter(obj -> {
                    if (obj == null) return false;
                    return true;
                })
                .map(obj -> obj.toString())
                .collect(Collectors.joining(", "));
    }

    public static String duplicateAndJoin(List<Object> objects) {
        return objects.stream()
                .flatMap(obj -> List.of(obj, obj).stream())
                .map(obj -> obj.toString())
                .collect(Collectors.joining(", "));
    }
}
