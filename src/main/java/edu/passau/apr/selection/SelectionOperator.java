package edu.passau.apr.selection;

import edu.passau.apr.model.FitnessResult;
import edu.passau.apr.model.Patch;

import java.util.List;
import java.util.Random;

/**
 * Selection operator for genetic algorithm.
 * Implements tournament selection.
 */
public class SelectionOperator {
    private final Random random;
    private final int tournamentSize;

    public SelectionOperator(Random random, int tournamentSize) {
        this.random = random;
        this.tournamentSize = tournamentSize;
    }

    /**
     * Selects a patch from the population using tournament selection.
     */
    public Patch select(List<Patch> population, List<FitnessResult> fitnesses) {
        Patch best = null;
        double bestFitness = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < tournamentSize; i++) {
            int index = random.nextInt(population.size());
            Patch candidate = population.get(index);
            double fitness = fitnesses.get(index).fitness();

            if (fitness > bestFitness) {
                bestFitness = fitness;
                best = candidate;
            }
        }

        return best != null ? best : population.get(0);
    }
}

