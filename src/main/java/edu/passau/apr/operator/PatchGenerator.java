package edu.passau.apr.operator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import edu.passau.apr.model.Edit;
import edu.passau.apr.model.Patch;
import edu.passau.apr.util.AstUtils;
import edu.passau.apr.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates initial candidates and crossover offspring for the genetic search.
 */
public class PatchGenerator {
    private static final int MAX_TARGET_STATEMENTS = 12;
    private static final int MAX_DONOR_STATEMENTS = 24;
    private static final int MAX_INSERT_DONORS_PER_TARGET = 4;
    private static final int MAX_SWAP_DONORS_PER_TARGET = 4;
    private static final int MAX_REPLACE_EXPRESSION_INDICES = 6;
    private static final int MAX_NEGATE_EXPRESSION_INDICES = 24;
    private static final int MAX_REPLACE_EDITS_PER_PAIR = 4;
    private static final int MAX_BINARY_EXPRESSION_INDICES = 6;
    private static final int MAX_BINARY_EDITS_PER_STATEMENT = 6;
    private static final int MAX_SINGLE_EDIT_POOL = 96;
    private static final int MAX_COMBINATION_POOL = 24;
    private static final int MAX_TWO_EDIT_ATTEMPTS = 120;
    private static final double SINGLE_EDIT_RATIO = 0.75;
    private static final int MAX_REPLACE_SEEDS = 30;
    private static final int MAX_BINARY_SEEDS = 24;
    private static final int MAX_NEGATE_SEEDS = 16;
    private static final int MAX_INSERT_SEEDS = 16;
    private static final int MAX_SWAP_SEEDS = 12;

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

    /**
     * Generates a random initial patch.
     */
    public Patch generateRandomPatch() {
        Patch patch = new Patch(source, weights);
        patch.doMutations(mutationWeight, random);
        return patch;
    }

    /**
     * Creates deterministic, generic seed patches.
     * Seeds are assembled from suspicious locations only, without benchmark-specific patterns.
     */
    public List<Patch> generateGuidedPatches(int maxCount) {
        if (maxCount <= 0 || sourceStatementCount <= 0) {
            return List.of();
        }

        List<Statement> statements = mutableStatements();
        if (statements.isEmpty()) {
            return List.of();
        }

        List<Integer> targetIndices = prioritizedStatementIndices(statements);
        if (targetIndices.size() > MAX_TARGET_STATEMENTS) {
            targetIndices = new ArrayList<>(targetIndices.subList(0, MAX_TARGET_STATEMENTS));
        }

        List<Integer> donorIndices = prioritizedDonorIndices(statements);
        if (donorIndices.size() > MAX_DONOR_STATEMENTS) {
            donorIndices = new ArrayList<>(donorIndices.subList(0, MAX_DONOR_STATEMENTS));
        }

        List<Edit> singleEdits = generateSingleEditCandidates(statements, targetIndices, donorIndices);
        List<Patch> guided = new ArrayList<>();
        Set<String> seenPrograms = new HashSet<>();

        int singleQuota = Math.max(1, (int) Math.round(maxCount * SINGLE_EDIT_RATIO));
        for (Edit edit : singleEdits) {
            if (guided.size() >= singleQuota || guided.size() >= maxCount) {
                break;
            }
            Patch candidate = new Patch(source, weights);
            if (candidate.applyEdit(edit) && remember(candidate, seenPrograms)) {
                guided.add(candidate);
            }
        }

        if (guided.size() < maxCount) {
            addTwoEditSeeds(singleEdits, guided, seenPrograms, maxCount);
        }

        return guided;
    }

    private List<Edit> generateSingleEditCandidates(List<Statement> statements,
                                                    List<Integer> targetIndices,
                                                    List<Integer> donorIndices) {
        List<Edit> deletes = new ArrayList<>();
        List<Edit> replaces = new ArrayList<>();
        List<Edit> binaries = new ArrayList<>();
        List<Edit> negates = new ArrayList<>();
        List<Edit> inserts = new ArrayList<>();
        List<Edit> swaps = new ArrayList<>();
        Set<String> seenEdits = new HashSet<>();

        for (Integer targetIndex : targetIndices) {
            addIfApplicable(deletes, seenEdits, new Edit(Edit.Type.DELETE, targetIndex, null));
        }

        for (Integer targetIndex : targetIndices) {
            for (int donorPos = 0; donorPos < donorIndices.size() && donorPos < MAX_INSERT_DONORS_PER_TARGET; donorPos++) {
                Integer donorIndex = donorIndices.get(donorPos);
                if (!targetIndex.equals(donorIndex)) {
                    Edit insertEdit = new Edit(Edit.Type.INSERT, targetIndex, donorIndex);
                    addIfApplicable(inserts, seenEdits, insertEdit);
                }
            }

            for (int donorPos = 0; donorPos < donorIndices.size() && donorPos < MAX_SWAP_DONORS_PER_TARGET; donorPos++) {
                Integer donorIndex = donorIndices.get(donorPos);
                if (!targetIndex.equals(donorIndex)) {
                    Edit swapEdit = new Edit(Edit.Type.SWAP, targetIndex, donorIndex);
                    addIfApplicable(swaps, seenEdits, swapEdit);
                }
            }
        }

        for (Integer targetIndex : targetIndices) {
            Statement targetStatement = statements.get(targetIndex);
            List<Integer> targetExpressionIndices = prioritizedExpressionIndices(targetStatement, MAX_REPLACE_EXPRESSION_INDICES);
            if (targetExpressionIndices.isEmpty()) {
                continue;
            }

            List<Integer> candidateDonors = prioritizedReplaceDonors(targetIndex, donorIndices, statements);

            for (Integer donorIndex : candidateDonors) {
                Statement donorStatement = statements.get(donorIndex);
                List<Integer> donorExpressionIndices = prioritizedExpressionIndices(donorStatement, MAX_REPLACE_EXPRESSION_INDICES);
                if (donorExpressionIndices.isEmpty()) {
                    continue;
                }

                int producedForPair = 0;
                for (Integer targetExprIndex : targetExpressionIndices) {
                    for (Integer donorExprIndex : donorExpressionIndices) {
                        if (targetIndex.equals(donorIndex) && targetExprIndex.equals(donorExprIndex)) {
                            continue;
                        }

                        Edit replaceEdit = new Edit(
                            Edit.Type.REPLACE_EXPR,
                            targetIndex,
                            donorIndex,
                            targetExprIndex,
                            donorExprIndex
                        );
                        addIfApplicable(replaces, seenEdits, replaceEdit);

                        producedForPair++;
                        if (producedForPair >= MAX_REPLACE_EDITS_PER_PAIR) {
                            break;
                        }
                    }
                    if (producedForPair >= MAX_REPLACE_EDITS_PER_PAIR) {
                        break;
                    }
                }
            }
        }

        for (Integer targetIndex : targetIndices) {
            Statement targetStatement = statements.get(targetIndex);
            List<Expression> replaceableExpressions = getReplaceableExpressions(targetStatement);
            if (replaceableExpressions.isEmpty()) {
                continue;
            }

            List<Integer> binaryExpressionIndices = prioritizedBinaryExpressionIndices(
                replaceableExpressions,
                MAX_BINARY_EXPRESSION_INDICES
            );

            int producedForStatement = 0;
            for (Integer expressionIndex : binaryExpressionIndices) {
                BinaryExpr binaryExpr = replaceableExpressions.get(expressionIndex).asBinaryExpr();
                for (BinaryExpr.Operator operator : candidateOperators(binaryExpr.getOperator())) {
                    Edit edit = new Edit(
                        Edit.Type.MUTATE_BINARY_OPERATOR,
                        targetIndex,
                        null,
                        expressionIndex,
                        binaryOperatorCode(operator)
                    );
                    addIfApplicable(binaries, seenEdits, edit);
                    producedForStatement++;
                    if (producedForStatement >= MAX_BINARY_EDITS_PER_STATEMENT) {
                        break;
                    }
                }
                if (producedForStatement >= MAX_BINARY_EDITS_PER_STATEMENT) {
                    break;
                }
            }
        }

        for (Integer targetIndex : targetIndices) {
            Statement targetStatement = statements.get(targetIndex);
            List<Integer> targetExpressionIndices = prioritizedNegatableExpressionIndices(
                targetStatement,
                MAX_NEGATE_EXPRESSION_INDICES
            );
            for (Integer targetExprIndex : targetExpressionIndices) {
                Edit negateEdit = new Edit(Edit.Type.NEGATE_EXPRESSION, targetIndex, null, targetExprIndex, null);
                addIfApplicable(negates, seenEdits, negateEdit);
            }
        }

        List<Edit> edits = roundRobinMerge(
            binaries,
            replaces,
            negates,
            deletes,
            inserts,
            swaps
        );

        if (edits.size() > MAX_SINGLE_EDIT_POOL) {
            return new ArrayList<>(edits.subList(0, MAX_SINGLE_EDIT_POOL));
        }
        return edits;
    }

    private void addTwoEditSeeds(List<Edit> singleEdits, List<Patch> guided, Set<String> seenPrograms, int maxCount) {
        int pool = Math.min(MAX_COMBINATION_POOL, singleEdits.size());
        int attempts = 0;

        for (int i = 0; i < pool; i++) {
            for (int j = i + 1; j < pool; j++) {
                if (guided.size() >= maxCount || attempts >= MAX_TWO_EDIT_ATTEMPTS) {
                    return;
                }

                Edit first = singleEdits.get(i);
                Edit second = singleEdits.get(j);

                attempts++;
                tryAddCompositeSeed(first, second, guided, seenPrograms, maxCount);
                if (guided.size() >= maxCount) {
                    return;
                }

                attempts++;
                tryAddCompositeSeed(second, first, guided, seenPrograms, maxCount);
            }
        }
    }

    private void tryAddCompositeSeed(Edit first,
                                     Edit second,
                                     List<Patch> guided,
                                     Set<String> seenPrograms,
                                     int maxCount) {
        if (guided.size() >= maxCount) {
            return;
        }

        Patch candidate = new Patch(source, weights);
        if (!candidate.applyEdit(first)) {
            return;
        }
        if (!candidate.applyEdit(second)) {
            return;
        }

        if (remember(candidate, seenPrograms)) {
            guided.add(candidate);
        }
    }

    private boolean remember(Patch patch, Set<String> seenPrograms) {
        String key = patch.getCompilationUnit().toString();
        return seenPrograms.add(key);
    }

    private void addIfApplicable(List<Edit> edits, Set<String> seenEdits, Edit edit) {
        String key = editKey(edit);
        if (!seenEdits.add(key)) {
            return;
        }

        Patch candidate = new Patch(source, weights);
        if (!candidate.applyEdit(edit)) {
            return;
        }

        edits.add(edit);
    }

    private String editKey(Edit edit) {
        return edit.type()
            + ":" + edit.statementIndex()
            + ":" + edit.donorStatementIndex()
            + ":" + edit.targetExpressionIndex()
            + ":" + edit.donorExpressionIndex();
    }

    private List<Edit> roundRobinMerge(List<Edit> binaries,
                                       List<Edit> replaces,
                                       List<Edit> negates,
                                       List<Edit> deletes,
                                       List<Edit> inserts,
                                       List<Edit> swaps) {
        // Interleave operator pools so destructive edits do not dominate the initial population.
        List<EditPoolCursor> cursors = new ArrayList<>();
        cursors.add(new EditPoolCursor(binaries, MAX_BINARY_SEEDS));
        cursors.add(new EditPoolCursor(replaces, MAX_REPLACE_SEEDS));
        cursors.add(new EditPoolCursor(negates, MAX_NEGATE_SEEDS));
        cursors.add(new EditPoolCursor(deletes, MAX_SINGLE_EDIT_POOL));
        cursors.add(new EditPoolCursor(inserts, MAX_INSERT_SEEDS));
        cursors.add(new EditPoolCursor(swaps, MAX_SWAP_SEEDS));

        List<Edit> merged = new ArrayList<>();
        while (merged.size() < MAX_SINGLE_EDIT_POOL) {
            boolean addedInRound = false;
            for (EditPoolCursor cursor : cursors) {
                if (merged.size() >= MAX_SINGLE_EDIT_POOL) {
                    break;
                }
                Edit next = cursor.next();
                if (next != null) {
                    merged.add(next);
                    addedInRound = true;
                }
            }
            if (!addedInRound) {
                break;
            }
        }
        return merged;
    }

    private List<Integer> prioritizedExpressionIndices(Statement statement, int limit) {
        List<Expression> expressions = getReplaceableExpressions(statement);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparingDouble((Integer i) -> expressionPriority(expressions.get(i))).reversed());
        if (indices.size() > limit) {
            return new ArrayList<>(indices.subList(0, limit));
        }
        return indices;
    }

    private List<Integer> prioritizedBinaryExpressionIndices(List<Expression> expressions, int limit) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            if (expressions.get(i).isBinaryExpr()
                && !candidateOperators(expressions.get(i).asBinaryExpr().getOperator()).isEmpty()) {
                indices.add(i);
            }
        }
        indices.sort(Comparator.comparingDouble((Integer i) -> expressionPriority(expressions.get(i))).reversed());
        if (indices.size() > limit) {
            return new ArrayList<>(indices.subList(0, limit));
        }
        return indices;
    }

    private List<Integer> prioritizedNegatableExpressionIndices(Statement statement, int limit) {
        List<Expression> expressions = getNegatableExpressions(statement);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < expressions.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparingDouble((Integer i) -> expressionPriority(expressions.get(i))).reversed());
        if (indices.size() > limit) {
            return new ArrayList<>(indices.subList(0, limit));
        }
        return indices;
    }

    private List<Expression> getReplaceableExpressions(Statement statement) {
        return statement.findAll(Expression.class).stream()
            .filter(AstUtils::isReplaceableExpression)
            .toList();
    }

    private List<Expression> getNegatableExpressions(Statement statement) {
        return statement.findAll(Expression.class).stream()
            .filter(AstUtils::isNegatableExpression)
            .toList();
    }

    private double expressionPriority(Expression expression) {
        if (expression.getParentNode().isPresent()
            && expression.getParentNode().get() instanceof com.github.javaparser.ast.expr.ConditionalExpr conditional
            && (conditional.getThenExpr() == expression || conditional.getElseExpr() == expression)) {
            return 3.6;
        }
        if (expression.isFieldAccessExpr()) {
            return 2.4;
        }
        if (expression.isArrayAccessExpr()) {
            return 2.8;
        }
        if (expression.isBinaryExpr()) {
            BinaryExpr.Operator operator = expression.asBinaryExpr().getOperator();
            return switch (operator) {
                case LESS, LESS_EQUALS, GREATER, GREATER_EQUALS, EQUALS, NOT_EQUALS -> 3.0;
                default -> 1.8;
            };
        }
        if (expression.isMethodCallExpr()) {
            return 2.6;
        }
        return 1.0;
    }

    private List<Integer> prioritizedReplaceDonors(int targetIndex,
                                                   List<Integer> donorIndices,
                                                   List<Statement> statements) {
        // Prefer donors from the same method to preserve local variable/type context.
        List<Integer> candidates = new ArrayList<>(donorIndices);
        if (!candidates.contains(targetIndex)) {
            candidates.add(targetIndex);
        }

        Statement target = statements.get(targetIndex);
        List<Integer> sameCallable = new ArrayList<>();
        List<Integer> others = new ArrayList<>();
        for (Integer donorIndex : candidates) {
            if (isSameEnclosingCallable(target, statements.get(donorIndex))) {
                sameCallable.add(donorIndex);
            } else {
                others.add(donorIndex);
            }
        }
        sameCallable.addAll(others);
        return sameCallable;
    }

    private List<BinaryExpr.Operator> candidateOperators(BinaryExpr.Operator operator) {
        List<BinaryExpr.Operator> options = switch (operator) {
            case LESS, LESS_EQUALS, GREATER, GREATER_EQUALS ->
                List.of(BinaryExpr.Operator.LESS, BinaryExpr.Operator.LESS_EQUALS,
                    BinaryExpr.Operator.GREATER, BinaryExpr.Operator.GREATER_EQUALS);
            case EQUALS, NOT_EQUALS ->
                List.of(BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS);
            default -> List.of();
        };
        return options.stream().filter(candidate -> candidate != operator).toList();
    }

    private int binaryOperatorCode(BinaryExpr.Operator operator) {
        return switch (operator) {
            case LESS -> 1;
            case LESS_EQUALS -> 2;
            case GREATER -> 3;
            case GREATER_EQUALS -> 4;
            case EQUALS -> 5;
            case NOT_EQUALS -> 6;
            default -> 0;
        };
    }

    private List<Statement> mutableStatements() {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(source);
            return compilationUnit.findAll(Statement.class)
                .stream()
                .filter(statement -> !statement.isBlockStmt())
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Integer> prioritizedStatementIndices(List<Statement> statements) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            indices.add(i);
        }

        indices.sort(Comparator
            .comparingDouble((Integer index) -> statementSuspiciousness(statements.get(index)))
            .reversed());
        return indices;
    }

    private List<Integer> prioritizedDonorIndices(List<Statement> statements) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator
            .comparingDouble((Integer index) -> donorScore(statements.get(index)))
            .reversed());
        return indices;
    }

    private double donorScore(Statement statement) {
        double score = statementSuspiciousness(statement);
        int expressionCount = getReplaceableExpressions(statement).size();
        score += Math.min(0.35, expressionCount * 0.05);
        if (statement.isExpressionStmt()) {
            score += 0.15;
        }
        if (statement.isReturnStmt()) {
            score += 0.10;
        }
        if (statement.isIfStmt()) {
            score += 0.08;
        }
        return score;
    }

    private double statementSuspiciousness(Statement statement) {
        if (statement.getRange().isPresent()) {
            var range = statement.getRange().get();
            double maxWeight = 0.0;
            for (int line = range.begin.line; line <= range.end.line; line++) {
                maxWeight = Math.max(maxWeight, weights.getOrDefault(line, 0.0));
            }
            if (maxWeight > 0.0) {
                return maxWeight;
            }
        }
        int line = statement.getBegin().map(position -> position.line).orElse(-1);
        if (line >= 0) {
            return weights.getOrDefault(line, 0.0);
        }
        return 0.0;
    }

    /**
     * Performs crossover between two parent patches.
     */
    public Pair<Patch, Patch> crossover(Patch p, Patch q) {
        if (sourceStatementCount <= 1) {
            return new Pair<>(p.copy(), q.copy());
        }

        List<Edit> normalizedP = normalizeScriptToSourceCoordinates(p.getEdits());
        List<Edit> normalizedQ = normalizeScriptToSourceCoordinates(q.getEdits());
        int cutoff = random.nextInt(sourceStatementCount);
        List<Edit> cEdits = buildOffspringScript(normalizedP, normalizedQ, cutoff);
        List<Edit> dEdits = buildOffspringScript(normalizedQ, normalizedP, cutoff);

        Patch c = new Patch(source, weights);
        Patch d = new Patch(source, weights);
        c.applyEdits(cEdits);
        d.applyEdits(dEdits);
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
        return type == Edit.Type.INSERT || type == Edit.Type.SWAP || type == Edit.Type.REPLACE_EXPR;
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
