import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntervalPlannerTest {

    @Test
    public void normalizeIntervals_ordersAndFilters() {
        List<int[]> normalized = IntervalPlanner.normalizeIntervals(Arrays.asList(
            new int[]{5, 1},
            new int[]{3, 3},
            new int[]{8, 10}
        ));

        assertEquals(2, normalized.size());
        assertArrayEquals(new int[]{1, 5}, normalized.get(0));
        assertArrayEquals(new int[]{8, 10}, normalized.get(1));
    }

    @Test
    public void mergeIntervals_mergesOverlappingAndAdjacent() {
        List<int[]> merged = IntervalPlanner.mergeIntervals(Arrays.asList(
            new int[]{1, 3},
            new int[]{3, 5},
            new int[]{9, 12},
            new int[]{10, 11}
        ));

        assertEquals(2, merged.size());
        assertArrayEquals(new int[]{1, 5}, merged.get(0));
        assertArrayEquals(new int[]{9, 12}, merged.get(1));
    }

    @Test
    public void invertWithinRange_findsFreeGaps() {
        List<int[]> free = IntervalPlanner.invertWithinRange(Arrays.asList(
            new int[]{9, 10},
            new int[]{12, 13}
        ), 8, 14);

        assertEquals(3, free.size());
        assertArrayEquals(new int[]{8, 9}, free.get(0));
        assertArrayEquals(new int[]{10, 12}, free.get(1));
        assertArrayEquals(new int[]{13, 14}, free.get(2));
    }

    @Test
    public void totalDuration_sumsDurations() {
        int total = IntervalPlanner.totalDuration(Arrays.asList(
            new int[]{1, 4},
            new int[]{8, 10},
            new int[]{12, 12}
        ));

        assertEquals(5, total);
    }

    @Test
    public void mergeIntervals_emptyInput() {
        assertEquals(0, IntervalPlanner.mergeIntervals(List.of()).size());
    }
}
