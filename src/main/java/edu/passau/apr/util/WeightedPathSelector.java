package edu.passau.apr.util;

import edu.passau.apr.model.StatementWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Selects statements based on fault localization weights.
 * Uses weighted random selection where statements with higher weights
 * have higher probability of being selected.
 */
public class WeightedPathSelector {
    private final List<StatementWeight> weights;
    private final Random random;
    private final List<Double> cumulativeWeights;

    public WeightedPathSelector(List<StatementWeight> weights, Random random) {
        this.weights = new ArrayList<>(weights);
        this.random = random;
        this.cumulativeWeights = computeCumulativeWeights();
    }

    private List<Double> computeCumulativeWeights() {
        List<Double> cumulative = new ArrayList<>();
        double sum = 0.0;
        for (StatementWeight sw : weights) {
            sum += sw.getWeight();
            cumulative.add(sum);
        }
        return cumulative;
    }

    /**
     * Selects a line number based on weighted probabilities.
     * Returns -1 if no valid statement can be selected.
     */
    public int selectLine() {
        if (weights.isEmpty() || cumulativeWeights.isEmpty()) {
            return -1;
        }

        double totalWeight = cumulativeWeights.get(cumulativeWeights.size() - 1);
        if (totalWeight <= 0.0) {
            // If all weights are zero, select uniformly
            if (weights.isEmpty()) {
                return -1;
            }
            return weights.get(random.nextInt(weights.size())).getLineNumber();
        }

        double randomValue = random.nextDouble() * totalWeight;
        for (int i = 0; i < cumulativeWeights.size(); i++) {
            if (randomValue <= cumulativeWeights.get(i)) {
                return weights.get(i).getLineNumber();
            }
        }

        // Fallback to last element
        return weights.get(weights.size() - 1).getLineNumber();
    }

    /**
     * Gets all line numbers that have non-zero weight.
     */
    public List<Integer> getWeightedLines() {
        List<Integer> lines = new ArrayList<>();
        for (StatementWeight sw : weights) {
            if (sw.getWeight() > 0.0) {
                lines.add(sw.getLineNumber());
            }
        }
        return lines;
    }
}

