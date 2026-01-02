package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Genetic operator that inserts a donor statement into the source code.
 */
public class InsertOperator extends GeneticOperator {
    public InsertOperator(Random random, WeightedPathSelector pathSelector, List<String> sourceLines) {
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

        return new Edit(Edit.Type.INSERT, lineNumber, donor);
    }
}

