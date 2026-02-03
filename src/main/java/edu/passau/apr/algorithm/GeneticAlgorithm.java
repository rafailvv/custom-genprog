package edu.passau.apr.algorithm;

import com.github.javaparser.ast.CompilationUnit;
import edu.passau.apr.evaluator.FitnessEvaluator;
import edu.passau.apr.model.AstProgram;
import edu.passau.apr.model.FitnessResult;
import edu.passau.apr.model.Patch;
import edu.passau.apr.operator.PatchGenerator;
import edu.passau.apr.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.Collections.shuffle;

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
    private final AstProgram originalProgram;

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
                           AstProgram originalProgram) {
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.timeLimitMs = timeLimitMs;
        this.mutationWeight = mutationWeight;
        this.crossoverRate = crossoverRate;
        this.random = random;
        this.patchGenerator = patchGenerator;
        this.fitnessEvaluator = fitnessEvaluator;
        this.originalProgram = originalProgram;
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

        while (!isFinished()) {
            currentGeneration++;

            List<Patch> viablePatches = new ArrayList<>();
            List<Patch> newPopulation = new ArrayList<>();
            List<FitnessResult> viFit = new ArrayList<>();

            // Collect viable patches (where at least one test passes)
            // Viable ← { (P, PathP) in Popul | f(P) > 0 }
            for (int i = 0; i < population.size(); i++) {
                if (fitnesses.get(i).passingTests() > 0) {
                    viablePatches.add(population.get(i));
                    viFit.add(fitnesses.get(i));
                }
            }

            // crossover rate is 1 -> every surviving patch undergoes crossover (but only parent in one crossover generation)
            var pairs = pairUp(select(viablePatches, viFit, populationSize / 2));
            for (Pair<Patch, Patch> parents : pairs) {
                Pair<Patch, Patch> offspring = patchGenerator.crossover(parents.first(), parents.second());

                newPopulation.addAll(List.of(
                    parents.first(),
                    parents.second(),
                    offspring.first(),
                    offspring.second()
                ));
            }

            // mutate all patches in the new population
            newPopulation.forEach(p -> p.doMutations(mutationWeight, random));

            population = newPopulation;
            evaluatePopulation();
            logProgress();
        }

        return new AlgorithmResult(bestPatch, bestFitness, currentGeneration,
                                  System.currentTimeMillis() - startTime);
    }


    /**
     * Selects pairs of patches for reproduction.
     * Randomly forms pairs from the list of patches.
     */
    private List<Pair<Patch, Patch>> pairUp(List<Patch> patches) {
        List<Pair<Patch, Patch>> pairs = new ArrayList<>();
        List<Patch> shuffled = new ArrayList<>(patches);
        shuffle(shuffled, random);

        if (shuffled.size() < 2) {
            return List.of(new Pair<>(shuffled.get(0), shuffled.get(0)));
        }

        for (int i = 0; i < shuffled.size() - 1; i += 2) {
            pairs.add(new Pair<>(shuffled.get(i), shuffled.get(i + 1)));
        }

        return pairs;
    }

    /**
     * Selects selectionSize patches from viablePatches based on their fitness.
     */
    private List<Patch> select(List<Patch> viablePatches, List<FitnessResult> fitnesses, int selectionSize) {
        List<Patch> selectedPatches = new ArrayList<>();
        List<FitnessResult> selectedFs = new ArrayList<>();

        for (int i = 0; i < viablePatches.size(); i++) {
            if (selectedPatches.size() <= selectionSize) {
                selectedPatches.add(viablePatches.get(i));
                selectedFs.add(fitnesses.get(i));
            } else {
                int smallestFitnessIndex = 0;
                double smallestFitnessValue = selectedFs.get(0).fitness();
                for (int j = 0; j < selectedPatches.size(); j++) {
                    if (selectedFs.get(j).fitness() < smallestFitnessValue) {
                        smallestFitnessValue = selectedFs.get(j).fitness();
                        smallestFitnessIndex = j;
                    }
                }
                if (fitnesses.get(i).fitness() > smallestFitnessValue) {
                    selectedPatches.set(smallestFitnessIndex, viablePatches.get(i));
                    selectedFs.set(smallestFitnessIndex, fitnesses.get(i));
                }
            }
        }

        return selectedPatches;
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

        CompilationUnit originalCu = originalProgram.getCompilationUnit();

        for (Patch patch : population) {
            CompilationUnit patched = patch.applyTo(originalCu);
            FitnessResult fitness = fitnessEvaluator.evaluate(patch, patched.toString());
            fitnesses.add(fitness);

            if (bestFitness == null || fitness.fitness() > bestFitness.fitness()) {
                bestFitness = fitness;
                bestPatch = patch;
            }

            if (fitness.allTestsPass()) {
                break;
            }
        }
    }

    private boolean isFinished() {
        if (bestFitness != null && bestFitness.allTestsPass()) {
            return true;
        }

        if (currentGeneration >= maxGenerations) {
            return true;
        }

        return System.currentTimeMillis() - startTime >= timeLimitMs;
    }

    private void logProgress() {
        System.out.printf(
            "Generation %d: Best fitness = %.2f (Passing: %d, Failing: %d)%n",
            currentGeneration,
            bestFitness != null ? bestFitness.fitness() : Double.NEGATIVE_INFINITY,
            bestFitness != null ? bestFitness.passingTests() : 0,
            bestFitness != null ? bestFitness.failingTests() : 0
        );
        System.out.println("Best Patch so far:");
        System.out.println(bestPatch);
        System.out.println(bestPatch.applyTo(originalProgram.getCompilationUnit()));
        System.out.println("----------------------------------------");
    }

    public record AlgorithmResult(Patch bestPatch, FitnessResult bestFitness, int generations, long elapsedTimeMs) {
        public boolean foundSolution() {
            return bestFitness != null && bestFitness.allTestsPass();
        }
    }
}

