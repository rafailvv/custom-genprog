package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Generates random patches using genetic operators.
 */
public class PatchGenerator {
    private final Random random;
    private final WeightedPathSelector pathSelector;
    private final List<String> sourceLines;
    private final double mutationWeight;
    private final int maxEditsPerPatch;

    private final DeleteOperator deleteOp;
    private final InsertOperator insertOp;
    private final ReplaceOperator replaceOp;

    public PatchGenerator(Random random, WeightedPathSelector pathSelector, 
                         List<String> sourceLines, double mutationWeight, int maxEditsPerPatch) {
        this.random = random;
        this.pathSelector = pathSelector;
        this.sourceLines = sourceLines;
        this.mutationWeight = mutationWeight;
        this.maxEditsPerPatch = maxEditsPerPatch;

        this.deleteOp = new DeleteOperator(random, pathSelector, sourceLines);
        this.insertOp = new InsertOperator(random, pathSelector, sourceLines);
        this.replaceOp = new ReplaceOperator(random, pathSelector, sourceLines);
    }

    /**
     * Generates a random initial patch.
     */
    public Patch generateRandomPatch() {
        Patch patch = new Patch();
        int numEdits = 1 + random.nextInt(maxEditsPerPatch);

        for (int i = 0; i < numEdits; i++) {
            Edit edit = generateRandomEdit();
            if (edit != null) {
                patch.addEdit(edit);
            }
        }

        return patch;
    }

    /**
     * Mutates an existing patch.
     */
    public Patch mutate(Patch original) {
        Patch mutated = original.copy();

        if (random.nextDouble() < mutationWeight) {
            Edit newEdit = generateRandomEdit();
            if (newEdit != null) {
                mutated.addEdit(newEdit);
            }
        }

        if (!mutated.isEmpty() && random.nextDouble() < 0.3) {
            int editCount = mutated.getEditCount();
            if (editCount > 0) {
                int indexToRemove = random.nextInt(editCount);
                mutated.removeEdit(indexToRemove);
            }
        }

        while (mutated.getEditCount() > maxEditsPerPatch) {
            int editCount = mutated.getEditCount();
            if (editCount == 0) break;
            mutated.removeEdit(editCount - 1);
        }

        return mutated;
    }

    /**
     * Performs crossover between two parent patches.
     */
    public Patch crossover(Patch parent1, Patch parent2) {
        Patch child = new Patch();

        List<Edit> edits1 = parent1.getEdits();
        for (Edit edit : edits1) {
            if (random.nextDouble() < 0.5) {
                child.addEdit(edit);
            }
        }

        List<Edit> edits2 = parent2.getEdits();
        for (Edit edit : edits2) {
            if (random.nextDouble() < 0.5) {
                child.addEdit(edit);
            }
        }

        while (child.getEditCount() > maxEditsPerPatch) {
            int editCount = child.getEditCount();
            if (editCount == 0) break;
            child.removeEdit(editCount - 1);
        }

        return child;
    }

    private Edit generateRandomEdit() {
        double rand = random.nextDouble();
        
        if (rand < 0.33) {
            return deleteOp.apply();
        } else if (rand < 0.66) {
            return insertOp.apply();
        } else {
            return replaceOp.apply();
        }
    }
}

