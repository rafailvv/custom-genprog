import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogReportBuilderTest {

    private List<String> sampleLogs() {
        return Arrays.asList(
                "2026-01-01T10:00:00|/orders|200|120",
                "2026-01-01T10:00:01|/orders|500|340",
                "2026-01-01T10:00:02|/orders|502|300",
                "2026-01-01T10:00:03|/get-users|200|80",
                "2026-01-01T10:00:04|/get-users|404|70",
                "2026-01-01T10:00:05|/health|200|10",
                "2026-01-01T10:00:06|/get-users|500|90",
                "2026-01-01T10:00:07|/orders|200|110",
                "bad-row"
        );
    }

    @Test
    public void parseLines_filtersMalformedRows() {
        List<String[]> rows = LogReportBuilder.parseLines(sampleLogs());
        assertEquals(8, rows.size());
    }

    @Test
    public void aggregate_countsSuccessAndFailure() {
        Map<String, LogReportBuilder.EndpointStats> stats = LogReportBuilder.aggregate(sampleLogs());
        assertEquals(2, stats.get("/orders").successCount);
        assertEquals(2, stats.get("/orders").failCount);
        assertEquals(1, stats.get("/get-users").successCount);
        assertEquals(2, stats.get("/get-users").failCount);
    }

    @Test
    public void topFailingEndpoints_ordersByFailureCountThenLatency() {
        List<String> top = LogReportBuilder.topFailingEndpoints(sampleLogs(), 2);
        assertEquals("/orders#2#217", top.get(0));
        assertEquals("/get-users#2#80", top.get(1));
    }

    @Test
    public void buildSummaryLine_forUnknownPath() {
        assertEquals("/missing:0:0:0", LogReportBuilder.buildSummaryLine(sampleLogs(), "/missing"));
    }

    @Test
    public void buildSummaryLine_forKnownPath() {
        assertEquals("/get-users:1:2:80", LogReportBuilder.buildSummaryLine(sampleLogs(), "/get-users"));
    }
}
