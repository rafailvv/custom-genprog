package edu.passau.apr.model;

/**
 * Represents the fitness evaluation result for a patch.
 * Fitness is calculated based on passing and failing tests.
 */
public record FitnessResult(int passingTests, int failingTests, int totalTests, double fitness, boolean compiles,
                            boolean allTestsPass) {

    @Override
    public String toString() {
        return String.format("Fitness: %.2f (Passing: %d/%d, Failing: %d, Compiles: %s, AllPass: %s)",
                fitness, passingTests, totalTests, failingTests, compiles, allTestsPass);
    }
}

