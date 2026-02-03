package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import edu.passau.apr.util.Pair;

import java.util.List;
import java.util.Random;

/**
 * Generates random patches using genetic operators.
 */
public class PatchGenerator {
    private static final int DEFAULT_MAX_EDITS_PER_PATCH = 5;

    private final Random random;
    private final String source;
    private final double mutationWeight;

    public PatchGenerator(Random random,
                         String source, double mutationWeight) {
        this.random = random;
        this.source = source;
        this.mutationWeight = mutationWeight;
    }

    /**
     * Generates a random initial patch.
     */
    public Patch generateRandomPatch() {
        Patch patch = new Patch(source, sussyscory); // TODO
        patch.doMutations(mutationWeight, random);
        return patch;
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
}

