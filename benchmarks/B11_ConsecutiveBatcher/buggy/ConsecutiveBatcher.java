import java.util.ArrayList;
import java.util.List;

public class ConsecutiveBatcher {

    public static List<List<Integer>> groupConsecutive(List<Integer> sortedIds) {
        List<List<Integer>> groups = new ArrayList<>();
        if (sortedIds == null || sortedIds.isEmpty()) {
            return groups;
        }

        List<Integer> current = new ArrayList<>();
        current.add(sortedIds.get(0));

        for (int i = 1; i < sortedIds.size(); i++) {
            int previous = sortedIds.get(i - 1);
            int value = sortedIds.get(i);

            if (value == previous + 1) {
                current.add(value);
            } else {
                groups.add(current);
                current.clear();
                current.add(value);
            }
        }

        groups.add(new ArrayList<>(current));
        return groups;
    }

    public static int largestGroupSize(List<List<Integer>> groups) {
        int best = 0;
        if (groups == null) {
            return best;
        }

        for (List<Integer> group : groups) {
            best = Math.max(best, group.size());
        }

        return best;
    }

    public static List<Integer> flatten(List<List<Integer>> groups) {
        List<Integer> flattened = new ArrayList<>();
        if (groups == null) {
            return flattened;
        }

        for (List<Integer> group : groups) {
            flattened.addAll(group);
        }

        return flattened;
    }
}
