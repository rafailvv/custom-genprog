package edu.passau.apr.operator;

import edu.passau.apr.model.Edit;
import edu.passau.apr.util.WeightedPathSelector;

import java.util.List;
import java.util.Random;

/**
 * Genetic operator that deletes a statement from the source code.
 */
public class DeleteOperator extends GeneticOperator {
    public DeleteOperator(Random random, WeightedPathSelector pathSelector, List<String> sourceLines) {
        super(random, pathSelector, sourceLines);
    }

    @Override
    public Edit apply() {
        int lineNumber = pathSelector.selectLine();
        if (!isValidLine(lineNumber)) {
            return null;
        }
        return new Edit(Edit.Type.DELETE, lineNumber, null);
    }
}

