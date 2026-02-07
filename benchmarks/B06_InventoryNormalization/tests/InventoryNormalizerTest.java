import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InventoryNormalizerTest {

    @Test
    public void normalizeSku_removesSeparatorsAndUppercases() {
        assertEquals("AB1209", InventoryNormalizer.normalizeSku(" ab-12 09 "));
    }

    @Test
    public void normalizeSku_nullSafe() {
        assertEquals("", InventoryNormalizer.normalizeSku(null));
    }

    @Test
    public void applyEvents_tracksInboundOutboundPerSku() {
        List<InventoryNormalizer.StockEvent> events = Arrays.asList(
            new InventoryNormalizer.StockEvent("ab-1", 10, true, 1L),
            new InventoryNormalizer.StockEvent("AB1", 3, false, 2L),
            new InventoryNormalizer.StockEvent("ab 1", 4, true, 3L)
        );

        Map<String, Integer> stock = InventoryNormalizer.applyEvents(events);
        assertEquals(11, stock.get("AB1"));
    }

    @Test
    public void applyEvents_neverBelowZero() {
        List<InventoryNormalizer.StockEvent> events = Arrays.asList(
            new InventoryNormalizer.StockEvent("x-7", 2, true, 1L),
            new InventoryNormalizer.StockEvent("x7", 20, false, 2L)
        );

        Map<String, Integer> stock = InventoryNormalizer.applyEvents(events);
        assertEquals(0, stock.get("X7"));
    }

    @Test
    public void estimateDailyDemand_countsNormalizedSku() {
        Map<String, Integer> demand = InventoryNormalizer.estimateDailyDemand(
            Arrays.asList("a-1", "A1", "a 1", "b2")
        );

        assertEquals(3, demand.get("A1"));
        assertEquals(1, demand.get("B2"));
    }

    @Test
    public void buildReorderList_usesDemandAndSafety() {
        Map<String, Integer> stock = Map.of("A1", 6, "B2", 20, "C3", 1);
        Map<String, Integer> demand = Map.of("A1", 4, "B2", 3, "C3", 1);

        List<String> reorder = InventoryNormalizer.buildReorderList(stock, demand, 2, 1);
        assertEquals(Arrays.asList("A1", "C3"), reorder);
    }

    @Test
    public void buildReorderList_invalidHorizonReturnsEmpty() {
        List<String> reorder = InventoryNormalizer.buildReorderList(Map.of(), Map.of("A1", 1), 0, 2);
        assertTrue(reorder.isEmpty());
    }
}
