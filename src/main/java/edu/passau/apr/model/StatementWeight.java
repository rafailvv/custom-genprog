package edu.passau.apr.model;

/**
 * Represents a statement with its fault localization weight.
 */
public class StatementWeight {
    private final int lineNumber;
    private final double weight;

    public StatementWeight(int lineNumber, double weight) {
        this.lineNumber = lineNumber;
        this.weight = weight;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public double getWeight() {
        return weight;
    }
}

