package edu.passau.apr.algorithm;

import edu.passau.apr.evaluator.FitnessEvaluator;
import edu.passau.apr.model.FitnessResult;
import edu.passau.apr.model.Patch;
import edu.passau.apr.operator.PatchGenerator;
import edu.passau.apr.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static java.util.Collections.shuffle;

/**
 * Main genetic algorithm implementation for automated program repair.
 * Follows the GenProg algorithm structure.
 */
public class GeneticAlgorithm {
    private final int populationSize;
    private final int maxEliteSize;
    private final int maxGenerations;
    private final long timeLimitMs;
    private final double mutationWeight;
    private final double crossoverRate;
    private final Random random;
    private final PatchGenerator patchGenerator;
    private final FitnessEvaluator fitnessEvaluator;

    private List<Patch> population;
    private List<FitnessResult> fitnesses;
    private Patch bestPatch;
    private FitnessResult bestFitness;
    private int currentGeneration;
    private long startTime;

    public GeneticAlgorithm(int populationSize, int maxGenerations, long timeLimitMs,
                            double mutationWeight, double crossoverRate,
                            Random random, PatchGenerator patchGenerator,
                            FitnessEvaluator fitnessEvaluator) {
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.timeLimitMs = timeLimitMs;
        this.mutationWeight = mutationWeight;
        this.crossoverRate = crossoverRate;
        this.random = random;
        this.patchGenerator = patchGenerator;
        this.fitnessEvaluator = fitnessEvaluator;
        this.maxEliteSize = Math.max(2, populationSize / 10);
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

            System.out.println("=== Generation " + currentGeneration + " ===");
            for (int i = 0; i < population.size(); i++) {
                System.out.printf("Patch %d: Fitness = %.2f (Passing: %d, Failing: %d)%n",
                                  i, fitnesses.get(i).fitness(),
                                  fitnesses.get(i).passingTests(),
                                  fitnesses.get(i).failingTests());
                System.out.println(population.get(i));
                // System.out.println(population.get(i).getCompilationUnit().toString());
                System.out.println("---");
            }

            List<Patch> viablePatches = new ArrayList<>();
            List<Patch> newPopulation = new ArrayList<>();
            List<FitnessResult> viFit = new ArrayList<>();
            List<Patch> elitePatches  = new ArrayList<>();

            // Collect viable patches (where at least one test passes)
            // Viable ← { (P, PathP) in Popul | f(P) > 0 }
            for (int i = 0; i < population.size(); i++) {
                if (fitnesses.get(i).passingTests() > 0) {
                    viablePatches.add(population.get(i));
                    viFit.add(fitnesses.get(i));
                }
            }

            int currentEliteSize = Math.min(maxEliteSize, viablePatches.size());
            if (currentEliteSize > 0) {
                elitePatches = getTopPatches(viablePatches, fitnesses, currentEliteSize);
            }

            System.out.println("Current elite patches:");
            elitePatches.forEach(System.out::println);

            // crossover
            List<Patch> selectedParents = select(viablePatches, viFit, populationSize / 2);
            if (selectedParents.isEmpty()) {
                selectedParents = select(population, fitnesses, Math.max(2, populationSize / 2));
            }

            var pairs = pairUp(selectedParents);
            for (Pair<Patch, Patch> parents : pairs) {
                Patch parentA = parents.first();
                Patch parentB = parents.second();
                Pair<Patch, Patch> offspring;

                if (random.nextDouble() < crossoverRate) {
                    offspring = patchGenerator.crossover(parentA, parentB);
                } else {
                    offspring = new Pair<>(parentA.copy(), parentB.copy());
                }

                newPopulation.addAll(List.of(
                    parentA.copy(),
                    parentB.copy(),
                    offspring.first(),
                    offspring.second()
                ));
            }

            while (newPopulation.size() < populationSize) {
                newPopulation.add(patchGenerator.generateRandomPatch());
            }

            if (newPopulation.size() > populationSize) {
                newPopulation = new ArrayList<>(newPopulation.subList(0, populationSize));
            }

            // mutate all patches in the new population
            newPopulation.forEach(p -> p.doMutations(mutationWeight, random));

            // Keep the strongest variants unchanged (elitism) to avoid losing good repairs.
            for (int i = 0; i < elitePatches.size() && i < newPopulation.size(); i++) {
                newPopulation.set(i, elitePatches.get(i).copy());
            }

            population = newPopulation;
            evaluatePopulation();
            logProgress();
        }

        return new AlgorithmResult(bestPatch, bestFitness, currentGeneration, System.currentTimeMillis() - startTime);
    }


    /**
     * Selects pairs of patches for reproduction.
     * Randomly forms pairs from the list of patches.
     */
    private List<Pair<Patch, Patch>> pairUp(List<Patch> patches) {
        List<Pair<Patch, Patch>> pairs = new ArrayList<>();
        List<Patch> shuffled = new ArrayList<>(patches);
        shuffle(shuffled, random);

        if (shuffled.isEmpty()) {
            return List.of();
        }

        if (shuffled.size() == 1) {
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
        if (selectionSize <= 0 || viablePatches.isEmpty()) {
            return List.of();
        }

        int tournamentSize = Math.min(3, viablePatches.size());
        List<Patch> selectedPatches = new ArrayList<>(selectionSize);

        for (int i = 0; i < selectionSize; i++) {
            int winner = random.nextInt(viablePatches.size());
            for (int j = 1; j < tournamentSize; j++) {
                int contender = random.nextInt(viablePatches.size());
                FitnessResult winnerFitness = fitnesses.get(winner);
                FitnessResult contenderFitness = fitnesses.get(contender);
                if (isBetter(contenderFitness, winnerFitness)
                    || (sameQuality(contenderFitness, winnerFitness) && random.nextBoolean())) {
                    winner = contender;
                }
            }
            selectedPatches.add(viablePatches.get(winner));
        }

        return selectedPatches;
    }

    private void initializePopulation() {
        population = new ArrayList<>();
        int guidedCount = Math.min(populationSize, Math.max(6, (populationSize * 2) / 3));
        List<Patch> guidedSeeds = patchGenerator.generateGuidedPatches(guidedCount);
        population.addAll(guidedSeeds);

        for (int i = population.size(); i < populationSize; i++) {
            Patch patch = patchGenerator.generateRandomPatch();
            population.add(patch);
        }
    }

    private void evaluatePopulation() {
        fitnesses = new ArrayList<>();
        bestFitness = null;
        bestPatch = null;

        for (Patch patch : population) {
            String patchSrc = patch.getCompilationUnit().toString();
            FitnessResult fitness = fitnessEvaluator.evaluate(patchSrc);
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
        if (bestPatch != null) {
            System.out.println(bestPatch);
        } else {
            System.out.println("No patch available in this generation.");
        }
        System.out.println("----------------------------------------");
    }

    private List<Patch> getTopPatches(List<Patch> patches, List<FitnessResult> patchFitnesses, int limit) {
        if (limit <= 0 || patches.isEmpty() || patchFitnesses.isEmpty()) {
            return List.of();
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < patches.size() && i < patchFitnesses.size(); i++) {
            indices.add(i);
        }

        indices.sort(Comparator.comparingDouble((Integer i) -> patchFitnesses.get(i).fitness()).reversed());
        List<Patch> best = new ArrayList<>();
        for (int i = 0; i < limit && i < indices.size(); i++) {
            best.add(patches.get(indices.get(i)).copy());
        }
        return best;
    }

    private boolean hasAnyPassingTests(List<FitnessResult> patchFitnesses) {
        for (FitnessResult fitnessResult : patchFitnesses) {
            if (fitnessResult.passingTests() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean sameQuality(FitnessResult a, FitnessResult b) {
        return Double.compare(a.fitness(), b.fitness()) == 0
            && a.passingTests() == b.passingTests()
            && a.failingTests() == b.failingTests();
    }

    private boolean isBetter(FitnessResult contender, FitnessResult current) {
        if (contender.fitness() != current.fitness()) {
            return contender.fitness() > current.fitness();
        }
        if (contender.passingTests() != current.passingTests()) {
            return contender.passingTests() > current.passingTests();
        }
        if (contender.failingTests() != current.failingTests()) {
            return contender.failingTests() < current.failingTests();
        }
        if (contender.compiles() != current.compiles()) {
            return contender.compiles();
        }
        return false;
    }

    public record AlgorithmResult(Patch bestPatch, FitnessResult bestFitness, int generations, long elapsedTimeMs) {
        public boolean foundSolution() {
            return bestFitness != null && bestFitness.allTestsPass();
        }
    }
}
