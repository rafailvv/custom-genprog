package edu.passau.apr.model;

/**
 * Represents the fitness evaluation result for a patch.
 * Fitness is calculated based on passing and failing tests.
 */
public class FitnessResult {
    private final int passingTests;
    private final int failingTests;
    private final int totalTests;
    private final double fitness;
    private final boolean compiles;
    private final boolean allTestsPass;

    public FitnessResult(int passingTests, int failingTests, int totalTests, 
                        double fitness, boolean compiles, boolean allTestsPass) {
        this.passingTests = passingTests;
        this.failingTests = failingTests;
        this.totalTests = totalTests;
        this.fitness = fitness;
        this.compiles = compiles;
        this.allTestsPass = allTestsPass;
    }

    public int getPassingTests() {
        return passingTests;
    }

    public int getFailingTests() {
        return failingTests;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public double getFitness() {
        return fitness;
    }

    public boolean compiles() {
        return compiles;
    }

    public boolean allTestsPass() {
        return allTestsPass;
    }

    @Override
    public String toString() {
        return String.format("Fitness: %.2f (Passing: %d/%d, Failing: %d, Compiles: %s, AllPass: %s)",
                fitness, passingTests, totalTests, failingTests, compiles, allTestsPass);
    }
}

