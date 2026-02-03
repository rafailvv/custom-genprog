package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Genetic operator that replaces a statement with a donor statement.
 *
 * TODO: According to the paper this should be called SWAP and exchange one statement with another.
 */
public class ReplaceOperator extends GeneticOperator {
    public ReplaceOperator(Random random, WeightedPathSelector pathSelector, List<String> sourceLines) {
        super(random, pathSelector, sourceLines);
    }

    @Override
    public Edit apply() {
        int lineNumber = pathSelector.selectLine();
        if (!isValidLine(lineNumber)) {
            return null;
        }

        String donor = getDonorStatement();
        if (donor.isEmpty()) {
            return null;
        }

        return new Edit(Edit.Type.REPLACE, lineNumber, donor);
    }
}

