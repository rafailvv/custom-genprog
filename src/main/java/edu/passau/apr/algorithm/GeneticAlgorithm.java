package edu.passau.apr.algorithm;

import edu.passau.apr.evaluator.FitnessEvaluator;
import edu.passau.apr.model.FitnessResult;
import edu.passau.apr.model.Patch;
import edu.passau.apr.operator.PatchGenerator;
import edu.passau.apr.selection.SelectionOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Main genetic algorithm implementation for automated program repair.
 * Follows the GenProg algorithm structure.
 */
public class GeneticAlgorithm {
    private final int populationSize;
    private final int maxGenerations;
    private final long timeLimitMs;
    private final double mutationWeight;
    private final double crossoverRate;
    private final Random random;
    private final PatchGenerator patchGenerator;
    private final FitnessEvaluator fitnessEvaluator;
    private final SelectionOperator selectionOperator;
    private final List<String> originalSourceLines;

    private List<Patch> population;
    private List<FitnessResult> fitnesses;
    private Patch bestPatch;
    private FitnessResult bestFitness;
    private int currentGeneration;
    private long startTime;

    public GeneticAlgorithm(int populationSize, int maxGenerations, long timeLimitMs,
                           double mutationWeight, double crossoverRate,
                           Random random, PatchGenerator patchGenerator,
                           FitnessEvaluator fitnessEvaluator,
                           SelectionOperator selectionOperator,
                           List<String> originalSourceLines) {
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.timeLimitMs = timeLimitMs;
        this.mutationWeight = mutationWeight;
        this.crossoverRate = crossoverRate;
        this.random = random;
        this.patchGenerator = patchGenerator;
        this.fitnessEvaluator = fitnessEvaluator;
        this.selectionOperator = selectionOperator;
        this.originalSourceLines = originalSourceLines;
    }

    /**
     * Runs the genetic algorithm until a solution is found or limits are reached.
     */
    public AlgorithmResult run() {
        startTime = System.currentTimeMillis();
        currentGeneration = 0;

        initializePopulation();
        evaluatePopulation();
        logProgress();

        while (!shouldStop()) {
            currentGeneration++;

            List<Patch> newPopulation = new ArrayList<>();

            if (bestPatch != null) {
                newPopulation.add(bestPatch.copy());
            }

            while (newPopulation.size() < populationSize) {
                Patch offspring;

                if (random.nextDouble() < crossoverRate && population.size() >= 2) {
                    Patch parent1 = selectionOperator.select(population, fitnesses);
                    Patch parent2 = selectionOperator.select(population, fitnesses);
                    offspring = patchGenerator.crossover(parent1, parent2);
                } else {
                    Patch parent = selectionOperator.select(population, fitnesses);
                    offspring = patchGenerator.mutate(parent);
                }

                newPopulation.add(offspring);
            }

            population = newPopulation;
            evaluatePopulation();
            logProgress();
        }

        return new AlgorithmResult(bestPatch, bestFitness, currentGeneration, 
                                  System.currentTimeMillis() - startTime);
    }

    private void initializePopulation() {
        population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            Patch patch = patchGenerator.generateRandomPatch();
            population.add(patch);
        }
    }

    private void evaluatePopulation() {
        fitnesses = new ArrayList<>();
        bestFitness = null;
        bestPatch = null;

        for (Patch patch : population) {
            FitnessResult fitness = fitnessEvaluator.evaluate(patch, originalSourceLines);
            fitnesses.add(fitness);

            if (bestFitness == null || fitness.getFitness() > bestFitness.getFitness()) {
                bestFitness = fitness;
                bestPatch = patch;
            }

            if (fitness.allTestsPass()) {
                break;
            }
        }
    }

    private boolean shouldStop() {
        if (bestFitness != null && bestFitness.allTestsPass()) {
            return true;
        }

        if (currentGeneration >= maxGenerations) {
            return true;
        }

        if (System.currentTimeMillis() - startTime >= timeLimitMs) {
            return true;
        }

        return false;
    }

    private void logProgress() {
        System.out.println(String.format(
            "Generation %d: Best fitness = %.2f (Passing: %d, Failing: %d)",
            currentGeneration,
            bestFitness != null ? bestFitness.getFitness() : Double.NEGATIVE_INFINITY,
            bestFitness != null ? bestFitness.getPassingTests() : 0,
            bestFitness != null ? bestFitness.getFailingTests() : 0
        ));
        System.out.flush();
    }

    public static class AlgorithmResult {
        private final Patch bestPatch;
        private final FitnessResult bestFitness;
        private final int generations;
        private final long elapsedTimeMs;

        public AlgorithmResult(Patch bestPatch, FitnessResult bestFitness, 
                              int generations, long elapsedTimeMs) {
            this.bestPatch = bestPatch;
            this.bestFitness = bestFitness;
            this.generations = generations;
            this.elapsedTimeMs = elapsedTimeMs;
        }

        public Patch getBestPatch() {
            return bestPatch;
        }

        public FitnessResult getBestFitness() {
            return bestFitness;
        }

        public int getGenerations() {
            return generations;
        }

        public long getElapsedTimeMs() {
            return elapsedTimeMs;
        }

        public boolean foundSolution() {
            return bestFitness != null && bestFitness.allTestsPass();
        }
    }
}

