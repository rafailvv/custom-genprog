package edu.passau.apr.config;

/**
 * Configuration parameters for the APR tool.
 */
public class Config {
    private String benchmarkPath;
    private long seed;
    private int maxGenerations;
    private long timeLimitSec;
    private int populationSize;
    private double positiveTestWeight;
    private double negativeTestWeight;
    private double mutationWeight;
    private boolean verbose;

    // Default values
    public static final int DEFAULT_POPULATION_SIZE = 40;
    public static final double DEFAULT_POSITIVE_TEST_WEIGHT = 1.0;
    public static final double DEFAULT_NEGATIVE_TEST_WEIGHT = 10.0;
    public static final double DEFAULT_MUTATION_WEIGHT = 0.06;
    public static final int DEFAULT_MAX_GENERATIONS = 50;
    public static final long DEFAULT_TIME_LIMIT_SEC = 60;

    public Config() {
        this.populationSize = DEFAULT_POPULATION_SIZE;
        this.positiveTestWeight = DEFAULT_POSITIVE_TEST_WEIGHT;
        this.negativeTestWeight = DEFAULT_NEGATIVE_TEST_WEIGHT;
        this.mutationWeight = DEFAULT_MUTATION_WEIGHT;
        this.maxGenerations = DEFAULT_MAX_GENERATIONS;
        this.timeLimitSec = DEFAULT_TIME_LIMIT_SEC;
        this.seed = System.currentTimeMillis();
        this.verbose = false;
    }

    public String getBenchmarkPath() {
        return benchmarkPath;
    }

    public void setBenchmarkPath(String benchmarkPath) {
        this.benchmarkPath = benchmarkPath;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getMaxGenerations() {
        return maxGenerations;
    }

    public void setMaxGenerations(int maxGenerations) {
        this.maxGenerations = maxGenerations;
    }

    public long getTimeLimitSec() {
        return timeLimitSec;
    }

    public void setTimeLimitSec(long timeLimitSec) {
        this.timeLimitSec = timeLimitSec;
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public void setPopulationSize(int populationSize) {
        this.populationSize = populationSize;
    }

    public double getPositiveTestWeight() {
        return positiveTestWeight;
    }

    public void setPositiveTestWeight(double positiveTestWeight) {
        this.positiveTestWeight = positiveTestWeight;
    }

    public double getNegativeTestWeight() {
        return negativeTestWeight;
    }

    public void setNegativeTestWeight(double negativeTestWeight) {
        this.negativeTestWeight = negativeTestWeight;
    }

    public double getMutationWeight() {
        return mutationWeight;
    }

    public void setMutationWeight(double mutationWeight) {
        this.mutationWeight = mutationWeight;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}

