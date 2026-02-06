import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConsecutiveBatcherTest {

    @Test
    public void groupConsecutive_splitsRunsCorrectly() {
        List<List<Integer>> groups = ConsecutiveBatcher.groupConsecutive(Arrays.asList(1, 2, 3, 7, 8, 11));
        assertEquals(List.of(1, 2, 3), groups.get(0));
        assertEquals(List.of(7, 8), groups.get(1));
        assertEquals(List.of(11), groups.get(2));
    }

    @Test
    public void groupConsecutive_handlesSingleRun() {
        List<List<Integer>> groups = ConsecutiveBatcher.groupConsecutive(Arrays.asList(4, 5, 6));
        assertEquals(1, groups.size());
        assertEquals(List.of(4, 5, 6), groups.get(0));
    }

    @Test
    public void largestGroupSize_findsMaximum() {
        List<List<Integer>> groups = ConsecutiveBatcher.groupConsecutive(Arrays.asList(1, 2, 10, 20, 21, 22));
        assertEquals(3, ConsecutiveBatcher.largestGroupSize(groups));
    }

    @Test
    public void flatten_rebuildsOriginalOrder() {
        List<List<Integer>> groups = ConsecutiveBatcher.groupConsecutive(Arrays.asList(2, 3, 9, 15, 16));
        assertEquals(List.of(2, 3, 9, 15, 16), ConsecutiveBatcher.flatten(groups));
    }

    @Test
    public void groupConsecutive_handlesNullInput() {
        assertEquals(0, ConsecutiveBatcher.groupConsecutive(null).size());
    }
}
