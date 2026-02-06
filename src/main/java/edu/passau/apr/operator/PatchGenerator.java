package edu.passau.apr.operator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import edu.passau.apr.model.StatementWeight;
import edu.passau.apr.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates random patches using genetic operators.
 */
public class PatchGenerator {
    private final Random random;
    private final String source;
    private final double mutationWeight;
    private final Map<Integer, Double> weights;
    private final int sourceStatementCount;

    public PatchGenerator(String source, Map<Integer, Double> weights, double mutationWeight, Random random) {
        this.random = random;
        this.source = source;
        this.mutationWeight = mutationWeight;
        this.weights = weights;
        this.sourceStatementCount = countMutableStatements(source);
    }

    private int countMutableStatements(String sourceCode) {
        try {
            return (int) StaticJavaParser.parse(sourceCode)
                    .findAll(Statement.class)
                    .stream()
                    .filter(statement -> !statement.isBlockStmt())
                    .count();
        } catch (Exception e) {
            return 0;
        }
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

        System.out.println("----------------- CROSSOVER -----------------");
        System.out.println("Performing crossover between patches with " + p.getEdits().size() + " and " + q.getEdits().size() + " edits.");
        System.out.println("Patch p source: \n" + p.getCompilationUnit().toString());
        System.out.println("Patch q source: \n" + q.getCompilationUnit().toString());

        List<Edit> normalizedP = normalizeScriptToSourceCoordinates(p.getEdits());
        List<Edit> normalizedQ = normalizeScriptToSourceCoordinates(q.getEdits());
        int cutoff = random.nextInt(sourceStatementCount);
        List<Edit> cEdits = buildOffspringScript(normalizedP, normalizedQ, cutoff);
        List<Edit> dEdits = buildOffspringScript(normalizedQ, normalizedP, cutoff);

        c.applyEdits(cEdits);
        d.applyEdits(dEdits);

        System.out.println("-----");
        System.out.println("Generated offspring patches with " + c.getEdits().size() + " and " + d.getEdits().size() + " edits.");
        System.out.println("Offspring c source: \n" + c.getCompilationUnit().toString());
        System.out.println("Offspring d source: \n" + d.getCompilationUnit().toString());
        System.out.println("---------------------------------------------");

        return new Pair<>(c, d);
    }

    private List<Edit> buildOffspringScript(List<Edit> leftParent, List<Edit> rightParent, int cutoff) {
        List<Edit> childScript = new ArrayList<>();

        // One-point crossover on edit locations.
        for (Edit edit : leftParent) {
            if (edit.statementIndex() <= cutoff) {
                childScript.add(edit);
            }
        }
        for (Edit edit : rightParent) {
            if (edit.statementIndex() > cutoff) {
                childScript.add(edit);
            }
        }

        return childScript;
    }

    private List<Edit> normalizeScriptToSourceCoordinates(List<Edit> script) {
        List<Edit> normalized = new ArrayList<>();
        List<Integer> positionToSource = new ArrayList<>(sourceStatementCount);
        for (int i = 0; i < sourceStatementCount; i++) {
            positionToSource.add(i);
        }

        int syntheticSeed = sourceStatementCount;
        for (Edit edit : script) {
            Integer targetSource = resolveSourceIndex(positionToSource, edit.statementIndex());
            if (targetSource == null) {
                continue;
            }

            Integer donorSource = null;
            if (requiresDonorStatement(edit.type())) {
                donorSource = resolveSourceIndex(positionToSource, edit.donorStatementIndex());
                if (donorSource == null) {
                    continue;
                }
            }

            Edit normalizedEdit = new Edit(
                    edit.type(),
                    targetSource,
                    donorSource,
                    edit.targetExpressionIndex(),
                    edit.donorExpressionIndex()
            );
            normalized.add(normalizedEdit);

            switch (edit.type()) {
                case DELETE -> removeAt(positionToSource, edit.statementIndex());
                case INSERT -> {
                    Integer donorPosition = edit.donorStatementIndex();
                    Integer donorIdentity = resolveSourceIndex(positionToSource, donorPosition);
                    if (isValidPosition(positionToSource, edit.statementIndex())) {
                        int identity = donorIdentity != null ? donorIdentity : syntheticSeed++;
                        positionToSource.add(edit.statementIndex(), identity);
                    }
                }
                case SWAP -> swapAt(positionToSource, edit.statementIndex(), edit.donorStatementIndex());
                default -> {
                    // REPLACE_EXPR / MUTATE_BINARY_OPERATOR / NEGATE_EXPRESSION do not change statement layout.
                }
            }
        }
        return normalized;
    }

    private Integer resolveSourceIndex(List<Integer> positionToSource, Integer positionIndex) {
        if (positionIndex == null || !isValidPosition(positionToSource, positionIndex)) {
            return null;
        }
        Integer sourceIndex = positionToSource.get(positionIndex);
        if (sourceIndex == null || sourceIndex < 0 || sourceIndex >= sourceStatementCount) {
            return null;
        }
        return sourceIndex;
    }

    private boolean requiresDonorStatement(Edit.Type type) {
        return type == Edit.Type.INSERT || type == Edit.Type.SWAP;
    }

    private boolean isValidPosition(List<Integer> positionToSource, int positionIndex) {
        return positionIndex >= 0 && positionIndex < positionToSource.size();
    }

    private void removeAt(List<Integer> positionToSource, int positionIndex) {
        if (isValidPosition(positionToSource, positionIndex)) {
            positionToSource.remove(positionIndex);
        }
    }

    private void swapAt(List<Integer> positionToSource, int first, Integer second) {
        if (second == null || !isValidPosition(positionToSource, first) || !isValidPosition(positionToSource, second)) {
            return;
        }
        Integer tmp = positionToSource.get(first);
        positionToSource.set(first, positionToSource.get(second));
        positionToSource.set(second, tmp);
    }

    private boolean isSameEnclosingCallable(Statement left, Statement right) {
        var leftCallable = left.findAncestor(CallableDeclaration.class).orElse(null);
        var rightCallable = right.findAncestor(CallableDeclaration.class).orElse(null);
        return leftCallable != null && leftCallable == rightCallable;
    }

    private static final class EditPoolCursor {
        private final List<Edit> edits;
        private final int limit;
        private int consumed;
        private int produced;

        private EditPoolCursor(List<Edit> edits, int limit) {
            this.edits = edits;
            this.limit = Math.max(0, limit);
            this.consumed = 0;
            this.produced = 0;
        }

        private Edit next() {
            if (produced >= limit || consumed >= edits.size()) {
                return null;
            }
            Edit edit = edits.get(consumed++);
            produced++;
            return edit;
        }
    }

}

