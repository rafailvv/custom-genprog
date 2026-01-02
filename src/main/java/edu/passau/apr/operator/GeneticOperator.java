package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Base class for genetic operators that modify patches.
 */
public abstract class GeneticOperator {
    protected final Random random;
    protected final WeightedPathSelector pathSelector;
    protected final List<String> sourceLines;

    public GeneticOperator(Random random, WeightedPathSelector pathSelector, List<String> sourceLines) {
        this.random = random;
        this.pathSelector = pathSelector;
        this.sourceLines = sourceLines;
    }

    /**
     * Applies the operator to create a new edit.
     */
    public abstract Edit apply();

    /**
     * Gets donor statements from the source code (excluding suspicious lines).
     */
    protected String getDonorStatement() {
        if (sourceLines.isEmpty()) {
            return "";
        }

        int donorIndex = random.nextInt(sourceLines.size());
        return sourceLines.get(donorIndex).trim();
    }

    /**
     * Checks if a line number is valid.
     */
    protected boolean isValidLine(int lineNumber) {
        return lineNumber >= 1 && lineNumber <= sourceLines.size();
    }
}

