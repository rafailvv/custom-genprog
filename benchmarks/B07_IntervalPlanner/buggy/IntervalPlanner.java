import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IntervalPlanner {

    public static List<int[]> normalizeIntervals(List<int[]> raw) {
        List<int[]> normalized = new ArrayList<>();
        if (raw == null) {
            return normalized;
        }

        for (int[] interval : raw) {
            if (interval == null || interval.length < 2) {
                continue;
            }
            int start = Math.min(interval[0], interval[1]);
            int end = Math.max(interval[0], interval[1]);
            if (start == end) {
                continue;
            }
            normalized.add(new int[]{start, end});
        }

        normalized.sort(Comparator.comparingInt(a -> a[0]));
        return normalized;
    }

    public static List<int[]> mergeIntervals(List<int[]> raw) {
        List<int[]> intervals = normalizeIntervals(raw);
        List<int[]> merged = new ArrayList<>();
        if (intervals.isEmpty()) {
            return merged;
        }

        int[] current = new int[]{intervals.get(0)[0], intervals.get(0)[1]};

        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] < current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                merged.add(current);
                current = new int[]{next[0], next[1]};
            }
        }

        merged.add(current);
        return merged;
    }

    public static List<int[]> invertWithinRange(List<int[]> busy, int dayStart, int dayEnd) {
        List<int[]> free = new ArrayList<>();
        if (dayStart >= dayEnd) {
            return free;
        }

        List<int[]> mergedBusy = mergeIntervals(busy);
        int cursor = dayStart;

        for (int[] interval : mergedBusy) {
            int start = Math.max(interval[0], dayStart);
            int end = Math.min(interval[1], dayEnd);

            if (start > cursor) {
                free.add(new int[]{cursor, start});
            }

            cursor = Math.max(cursor, end);
            if (cursor >= dayEnd) {
                break;
            }
        }

        if (cursor < dayEnd) {
            free.add(new int[]{cursor, dayEnd});
        }

        return free;
    }

    public static int totalDuration(List<int[]> intervals) {
        int total = 0;
        if (intervals == null) {
            return total;
        }

        for (int[] interval : intervals) {
            if (interval == null || interval.length < 2) {
                continue;
            }
            total += Math.max(interval[1] - interval[0], 0);
        }

        return total;
    }
}
