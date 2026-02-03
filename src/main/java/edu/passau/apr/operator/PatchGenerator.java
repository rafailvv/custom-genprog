package edu.passau.apr.operator;

import edu.passau.apr.model.Patch;
import edu.passau.apr.model.StatementWeight;
import edu.passau.apr.util.Pair;

import java.util.List;
import java.util.Random;

/**
 * Generates random patches using genetic operators.
 */
public class PatchGenerator {
    private final Random random;
    private final String source;
    private final double mutationWeight;
    private final List<StatementWeight> weights;

    public PatchGenerator(String source, List<StatementWeight> weights, double mutationWeight, Random random) {
        this.random = random;
        this.source = source;
        this.mutationWeight = mutationWeight;
        this.weights = weights;
    }

    /**
     * Generates a random initial patch.
     */
    public Patch generateRandomPatch() {
        Patch patch = new Patch(source, weights);
        patch.doMutations(mutationWeight, random);
        return patch;
    }

    /**
     * Performs crossover between two parent patches.
     */
    public Pair<Patch, Patch> crossover(Patch p, Patch q) {
        Patch c = new Patch(source, weights);
        Patch d = new Patch(source, weights);

        // swap edits around the cutoff line:
        // c = p[1..cutoff] + q[cutoff+1..end]
        // d = q[1..cutoff] + p[cutoff+1..end]

        // TODO

        return new Pair<>(c, d);
    }
}

