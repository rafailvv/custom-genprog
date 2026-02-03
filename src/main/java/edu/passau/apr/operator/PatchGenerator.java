package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import edu.passau.apr.util.Pair;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Generates random patches using genetic operators.
 */
public class PatchGenerator {
    private static final int DEFAULT_MAX_EDITS_PER_PATCH = 5;

    private final Random random;
    private final WeightedPathSelector pathSelector;
    private final List<String> sourceLines;
    private final double mutationWeight;

    private final DeleteOperator deleteOp;
    private final InsertOperator insertOp;
    private final ReplaceOperator replaceOp;

    public PatchGenerator(Random random, WeightedPathSelector pathSelector, 
                         List<String> sourceLines, double mutationWeight) {
        this.random = random;
        this.pathSelector = pathSelector;
        this.sourceLines = sourceLines;
        this.mutationWeight = mutationWeight;

        this.deleteOp = new DeleteOperator(random, pathSelector, sourceLines);
        this.insertOp = new InsertOperator(random, pathSelector, sourceLines);
        this.replaceOp = new ReplaceOperator(random, pathSelector, sourceLines);
    }

    /**
     * Generates a random initial patch.
     */
    public Patch generateRandomPatch() {
        Patch patch = new Patch();
        int numEdits = 1 + random.nextInt(DEFAULT_MAX_EDITS_PER_PATCH);

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

        // TODO instead of one new edit, it should loop through all statements and
        // mutate with probability mutationWeight * suspiciousness. See paper fig. 3.

        if (random.nextDouble() < mutationWeight) {
            Edit newEdit = generateRandomEdit();
            if (newEdit != null) {
                mutated.addEdit(newEdit);
            }
        }

        return mutated;
    }

    /**
     * Performs crossover between two parent patches.
     */
    public Pair<Patch, Patch> crossover(Patch p, Patch q) {
        Patch c = new Patch();
        Patch d = new Patch();

        int lineCount = sourceLines.size();
        int cutoff = random.nextInt(lineCount) + 1;

        // swap edits around the cutoff line:
        // c = p[1..cutoff] + q[cutoff+1..end]
        // d = q[1..cutoff] + p[cutoff+1..end]

        for (var edit : p.getEdits()) {
            if (edit.lineNumber() > cutoff) {
                d.addEdit(edit);
            } else {
                c.addEdit(edit);
            }
        }
        for (var edit : q.getEdits()) {
            if (edit.lineNumber() > cutoff) {
                c.addEdit(edit);
            } else {
                d.addEdit(edit);
            }
        }

        return new Pair<>(c, d);
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

