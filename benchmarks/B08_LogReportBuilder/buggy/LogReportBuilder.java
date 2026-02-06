import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogReportBuilder {

    public static class EndpointStats {
        public int successCount;
        public int failCount;
        public int totalLatency;

        public int totalCount() {
            return successCount + failCount;
        }

        public int avgLatency() {
            int total = totalCount();
            return total == 0 ? 0 : totalLatency / total;
        }
    }

    public static List<String[]> parseLines(List<String> lines) {
        List<String[]> rows = new ArrayList<>();
        if (lines == null) {
            return rows;
        }

        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 4) {
                continue;
            }

            String timestamp = parts[0].trim();
            String path = parts[1].trim();
            String status = parts[2].trim();
            String latencyMs = parts[3].trim();

            if (timestamp.isEmpty() || path.isEmpty() || status.isEmpty() || latencyMs.isEmpty()) {
                continue;
            }

            rows.add(new String[]{timestamp, path, status, latencyMs});
        }

        return rows;
    }

    public static Map<String, EndpointStats> aggregate(List<String> lines) {
        Map<String, EndpointStats> result = new HashMap<>();
        List<String[]> rows = parseLines(lines);

        for (String[] row : rows) {
            String path = row[1];
            String status = row[2];
            int latency;
            try {
                latency = Integer.parseInt(row[3]);
            } catch (NumberFormatException e) {
                continue;
            }

            EndpointStats stats = result.computeIfAbsent(path, k -> new EndpointStats());
            stats.totalLatency += Math.max(0, latency);

            if (status.startsWith("2")) {
                stats.successCount++;
            } else if (status.startsWith("4") || status.startsWith("5")) {
                stats.failCount++;
            }
        }

        return result;
    }

    public static List<String> topFailingEndpoints(List<String> lines, int limit) {
        List<String> result = new ArrayList<>();
        if (limit <= 0) {
            return result;
        }

        Map<String, EndpointStats> stats = aggregate(lines);
        List<Map.Entry<String, EndpointStats>> entries = new ArrayList<>(stats.entrySet());

        entries.sort((a, b) -> {
            int failCmp = Integer.compare(a.getValue().failCount, b.getValue().failCount);
            if (failCmp != 0) {
                return failCmp;
            }
            int latencyCmp = Integer.compare(b.getValue().avgLatency(), a.getValue().avgLatency());
            if (latencyCmp != 0) {
                return latencyCmp;
            }
            return a.getKey().compareTo(b.getKey());
        });

        for (Map.Entry<String, EndpointStats> entry : entries) {
            EndpointStats s = entry.getValue();
            if (s.failCount == 0) {
                continue;
            }
            result.add(entry.getKey() + "#" + s.failCount + "#" + s.avgLatency());
            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    public static String buildSummaryLine(List<String> lines, String path) {
        Map<String, EndpointStats> stats = aggregate(lines);
        EndpointStats s = stats.get(path);
        if (s == null) {
            return path + ":0:0:0";
        }
        return path + ":" + s.successCount + ":" + s.failCount + ":" + s.avgLatency();
    }
}
