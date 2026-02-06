import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryNormalizer {

    public static class StockEvent {
        public final String sku;
        public final int delta;
        public final boolean inbound;
        public final long timestamp;

        public StockEvent(String sku, int delta, boolean inbound, long timestamp) {
            this.sku = sku;
            this.delta = delta;
            this.inbound = inbound;
            this.timestamp = timestamp;
        }
    }

    public static String normalizeSku(String rawSku) {
        if (rawSku == null) {
            return "";
        }
        String trimmed = rawSku.trim().toUpperCase();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static Map<String, Integer> applyEvents(List<StockEvent> events) {
        Map<String, Integer> stockBySku = new HashMap<>();
        if (events == null) {
            return stockBySku;
        }

        for (StockEvent event : events) {
            if (event == null) {
                continue;
            }
            String sku = normalizeSku(event.sku);
            if (sku.isEmpty() || event.delta <= 0) {
                continue;
            }

            int previous = stockBySku.getOrDefault(sku, 0);
            int signedDelta = event.inbound ? event.delta : -event.delta;
            int next = previous + signedDelta;
            stockBySku.put(sku, Math.max(next, 0));
        }

        return stockBySku;
    }

    public static Map<String, Integer> estimateDailyDemand(List<String> soldSkus) {
        Map<String, Integer> demand = new HashMap<>();
        if (soldSkus == null) {
            return demand;
        }

        for (String rawSku : soldSkus) {
            String sku = normalizeSku(rawSku);
            if (sku.isEmpty()) {
                continue;
            }
            demand.put(sku, demand.getOrDefault(sku, 0) + 1);
        }

        return demand;
    }

    public static List<String> buildReorderList(Map<String, Integer> stock,
                                                Map<String, Integer> demand,
                                                int horizonDays,
                                                int safetyBuffer) {
        List<String> reorder = new ArrayList<>();
        if (stock == null || demand == null || horizonDays <= 0) {
            return reorder;
        }

        for (Map.Entry<String, Integer> entry : demand.entrySet()) {
            String sku = entry.getKey();
            int daily = Math.max(entry.getValue(), 0);
            int projectedNeed = daily * horizonDays + Math.max(0, safetyBuffer);
            int available = stock.getOrDefault(sku, 0);

            if (available < projectedNeed) {
                reorder.add(sku);
            }
        }

        reorder.sort(Comparator.naturalOrder());
        return reorder;
    }
}
