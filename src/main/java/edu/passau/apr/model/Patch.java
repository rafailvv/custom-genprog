package edu.passau.apr.model;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import edu.passau.apr.util.AstUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static edu.passau.apr.model.Edit.Type.DELETE;
import static edu.passau.apr.model.Edit.Type.INSERT;
import static edu.passau.apr.model.Edit.Type.MUTATE_BINARY_OPERATOR;
import static edu.passau.apr.model.Edit.Type.NEGATE_EXPRESSION;
import static edu.passau.apr.model.Edit.Type.REPLACE_EXPR;
import static edu.passau.apr.model.Edit.Type.SWAP;

/**
 * Represents a patch as a collection of edits.
 * A patch is applied to the buggy source code to create a candidate fix.
 */
public class Patch {
    private static final Range INVALID_RANGE = new Range(new Position(-1, -1), new Position(-1, -1));
    private static final int MAX_EDITS_PER_PATCH = 3;
    private static final double UNKNOWN_SUSPICIOUSNESS = 0.1;
    private static final double MIN_SUSPICIOUSNESS = 0.1;

    private final CompilationUnit compilationUnit;
    private final Map<Integer, Double> suspiciousness;
    private final boolean hasExactPositiveStatementWeight;
    private final List<Edit> edits = new ArrayList<>();

    public Patch(CompilationUnit cu, Map<Integer, Double> nodeWeights) {
        this.compilationUnit = cu;
        this.suspiciousness = nodeWeights;
        this.hasExactPositiveStatementWeight = computeHasExactPositiveStatementWeight();
    }

    public Patch(String source, Map<Integer, Double> nodeWeights) {
        this.compilationUnit = StaticJavaParser.parse(source);
        this.suspiciousness = nodeWeights;
        this.hasExactPositiveStatementWeight = computeHasExactPositiveStatementWeight();
    }

    public void doMutations(double mutationRate, Random random) {
        if (edits.size() >= MAX_EDITS_PER_PATCH) {
            return;
        }
        if (mutationRate <= 0.0) {
            return;
        }

        List<Statement> snapshot = new ArrayList<>(getMutableStatements());
        if (snapshot.isEmpty()) {
            return;
        }

        // GenProg-style mutation loop:
        // for each statement I_j, mutate it with probability mutationRate * W(I_j).
        for (Statement originalStatement : snapshot) {
            if (edits.size() >= MAX_EDITS_PER_PATCH) {
                break;
            }

            double weight = getSuspiciousness(originalStatement);
            if (weight <= 0.0) {
                continue;
            }
            if (random.nextDouble() > mutationRate) {
                continue;
            }
            if (random.nextDouble() > weight) {
                continue;
            }

            Integer currentIndex = currentStatementIndex(originalStatement);
            if (currentIndex == null) {
                continue;
            }

            Edit edit = createRandomEdit(currentIndex, random);
            if (edit != null) {
                applyEdit(edit);
            }
        }
    }

    public boolean applyEdit(Edit edit) {
        if (edits.size() >= MAX_EDITS_PER_PATCH) {
            return false;
        }
        if (!isMutableTargetIndex(edit.statementIndex())) {
            return false;
        }
        if (edit.type() == SWAP && (edit.donorStatementIndex() == null || !isMutableTargetIndex(edit.donorStatementIndex()))) {
            return false;
        }

        try {
            return switch (edit.type()) {
                case DELETE -> applyDelete(edit);
                case INSERT -> applyInsert(edit);
                case SWAP -> applySwap(edit);
                case REPLACE_EXPR -> applyReplaceExpression(edit);
                case MUTATE_BINARY_OPERATOR -> applyBinaryOperatorMutation(edit);
                case NEGATE_EXPRESSION -> applyNegateExpression(edit);
            };
        } catch (RuntimeException ex) {
            // Reject invalid AST rewrites but keep the search running.
            return false;
        }
    }

    public void applyEdits(List<Edit> candidateEdits) {
        for (Edit edit : candidateEdits) {
            applyEdit(edit);
        }
    }

    public List<Edit> getEdits() {
        return List.copyOf(edits);
    }

    public Patch copy() {
        Patch patchCopy = new Patch(compilationUnit.clone(), suspiciousness);
        patchCopy.edits.addAll(edits);
        return patchCopy;
    }

    private double getSuspiciousness(Statement statement) {
        // Get suspiciousness weight at the line number of the statement.
        // For multiline statements, the first line is chosen as this should have the highest suspiciousness value and
        // is definitely run.
        int line = statement.getBegin().map(position -> position.line).orElse(-1);
        var value = suspiciousness.getOrDefault(line, UNKNOWN_SUSPICIOUSNESS);
        if (value == 0) {
            // Do not assume perfect fault localization
            return MIN_SUSPICIOUSNESS;
        }
        return value;
    }

    private Edit createRandomEdit(int targetStatementIndex, Random random) {
        Edit.Type operation = chooseMutationOperation(random);

        return switch (operation) {
            case DELETE -> new Edit(DELETE, targetStatementIndex, null);
            case INSERT, SWAP -> {
                Integer donorStatementIndex = selectDonorStatementIndex(targetStatementIndex, operation, random);
                if (donorStatementIndex == null) {
                    yield null;
                }
                yield new Edit(operation, targetStatementIndex, donorStatementIndex);
            }
            case REPLACE_EXPR -> createReplaceExpressionEdit(targetStatementIndex, random);
            case MUTATE_BINARY_OPERATOR -> createBinaryOperatorMutationEdit(targetStatementIndex, random);
            case NEGATE_EXPRESSION -> createNegateExpressionEdit(targetStatementIndex, random);
        };
    }

    private Edit createNegateExpressionEdit(int targetStatementIndex, Random random) {
        Statement targetStatement = getMutableStatementAt(targetStatementIndex);
        if (targetStatement == null) {
            return null;
        }

        List<Expression> negatableExpressions = getNegatableExpressions(targetStatement);
        if (negatableExpressions.isEmpty()) {
            return null;
        }

        Integer targetExprIndex = selectExpressionIndex(negatableExpressions, random, true);
        if (targetExprIndex == null) {
            return null;
        }
        return new Edit(NEGATE_EXPRESSION, targetStatementIndex, null, targetExprIndex, null);
    }

    private Edit createBinaryOperatorMutationEdit(int targetStatementIndex, Random random) {
        Statement targetStatement = getMutableStatementAt(targetStatementIndex);
        if (targetStatement == null) {
            return null;
        }

        List<Expression> targetExpressions = getReplaceableExpressions(targetStatement);
        List<Integer> candidateBinaryIndices = new ArrayList<>();
        for (int i = 0; i < targetExpressions.size(); i++) {
            Expression expression = targetExpressions.get(i);
            if (expression.isBinaryExpr() && !candidateOperators(expression.asBinaryExpr().getOperator()).isEmpty()) {
                candidateBinaryIndices.add(i);
            }
        }
        if (candidateBinaryIndices.isEmpty()) {
            return null;
        }

        int targetExpressionIndex = candidateBinaryIndices.get(random.nextInt(candidateBinaryIndices.size()));
        BinaryExpr targetBinary = targetExpressions.get(targetExpressionIndex).asBinaryExpr();
        List<BinaryExpr.Operator> operators = candidateOperators(targetBinary.getOperator());
        if (operators.isEmpty()) {
            return null;
        }

        BinaryExpr.Operator newOperator = operators.get(random.nextInt(operators.size()));
        return new Edit(
            MUTATE_BINARY_OPERATOR,
            targetStatementIndex,
            null,
            targetExpressionIndex,
            binaryOperatorCode(newOperator)
        );
    }

    private Edit createReplaceExpressionEdit(int targetStatementIndex, Random random) {
        Integer donorStatementIndex = selectDonorStatementIndex(targetStatementIndex, REPLACE_EXPR, random);
        if (donorStatementIndex == null) {
            return null;
        }

        Statement targetStatement = getMutableStatementAt(targetStatementIndex);
        Statement donorStatement = getMutableStatementAt(donorStatementIndex);
        if (targetStatement == null || donorStatement == null) {
            return null;
        }

        List<Expression> targetExpressions = getReplaceableExpressions(targetStatement);
        List<Expression> donorExpressions = getReplaceableExpressions(donorStatement);
        if (targetExpressions.isEmpty() || donorExpressions.isEmpty()) {
            return null;
        }

        Integer targetExprIndex = selectExpressionIndex(targetExpressions, random, true);
        Integer donorExprIndex = selectExpressionIndex(donorExpressions, random, true);
        if (targetExprIndex == null || donorExprIndex == null) {
            return null;
        }

        if (targetStatementIndex == donorStatementIndex && targetExpressions.size() < 2) {
            return null;
        }

        if (targetStatementIndex == donorStatementIndex && targetExprIndex.equals(donorExprIndex)) {
            donorExprIndex = pickDifferentExpressionIndex(donorExpressions.size(), targetExprIndex, random);
            if (donorExprIndex == null) {
                return null;
            }
        }

        return new Edit(REPLACE_EXPR, targetStatementIndex, donorStatementIndex, targetExprIndex, donorExprIndex);
    }

    private Edit.Type chooseMutationOperation(Random random) {
        // GenProg core operator set is DELETE/INSERT/SWAP.
        // Keep a small extension window for expression-level mutations so Java benchmarks
        // requiring operator/value tweaks remain solvable without hardcoding.
        if (random.nextDouble() < 0.75) {
            return switch (random.nextInt(3)) {
                case 0 -> DELETE;
                case 1 -> INSERT;
                default -> SWAP;
            };
        }
        return switch (random.nextInt(3)) {
            case 0 -> REPLACE_EXPR;
            case 1 -> MUTATE_BINARY_OPERATOR;
            default -> NEGATE_EXPRESSION;
        };
    }

    private boolean applyDelete(Edit edit) {
        Statement target = getMutableStatementAt(edit.statementIndex());
        if (target == null) {
            return false;
        }
        target.remove();
        edits.add(edit);
        return true;
    }

    private boolean applyInsert(Edit edit) {
        Integer donorIndex = edit.donorStatementIndex();
        if (donorIndex == null) {
            return false;
        }

        Statement target = getMutableStatementAt(edit.statementIndex());
        Statement donor = getMutableStatementAt(donorIndex);
        if (target == null || donor == null) {
            return false;
        }

        if (!(target.getParentNode().orElse(null) instanceof BlockStmt parent)) {
            return false;
        }

        Statement donorClone = donor.clone();
        donorClone.setRange(INVALID_RANGE);
        parent.getStatements().addBefore(donorClone, target);
        edits.add(edit);
        return true;
    }

    private boolean applySwap(Edit edit) {
        Integer donorIndex = edit.donorStatementIndex();
        if (donorIndex == null || donorIndex == edit.statementIndex()) {
            return false;
        }

        Statement target = getMutableStatementAt(edit.statementIndex());
        Statement donor = getMutableStatementAt(donorIndex);
        if (target == null || donor == null || target == donor) {
            return false;
        }

        if (target.isAncestorOf(donor) || donor.isAncestorOf(target)) {
            return false;
        }

        Statement donorClone = donor.clone();
        Statement targetClone = target.clone();
        invalidateRanges(donorClone);
        invalidateRanges(targetClone);

        target.replace(donorClone);
        donor.replace(targetClone);
        edits.add(edit);
        return true;
    }

    private boolean applyReplaceExpression(Edit edit) {
        Integer donorStatementIndex = edit.donorStatementIndex();
        Integer targetExpressionIndex = edit.targetExpressionIndex();
        Integer donorExpressionIndex = edit.donorExpressionIndex();

        if (donorStatementIndex == null || targetExpressionIndex == null || donorExpressionIndex == null) {
            return false;
        }

        Statement targetStatement = getMutableStatementAt(edit.statementIndex());
        Statement donorStatement = getMutableStatementAt(donorStatementIndex);
        if (targetStatement == null || donorStatement == null) {
            return false;
        }

        List<Expression> targetExpressions = getReplaceableExpressions(targetStatement);
        List<Expression> donorExpressions = getReplaceableExpressions(donorStatement);
        if (targetExpressionIndex < 0 || targetExpressionIndex >= targetExpressions.size()) {
            return false;
        }
        if (donorExpressionIndex < 0 || donorExpressionIndex >= donorExpressions.size()) {
            return false;
        }

        Expression targetExpression = targetExpressions.get(targetExpressionIndex);
        Expression donorExpression = donorExpressions.get(donorExpressionIndex);
        if (targetExpression == donorExpression) {
            return false;
        }
        if (edit.statementIndex() == donorStatementIndex && targetExpressionIndex.equals(donorExpressionIndex)) {
            return false;
        }
        if (!areCompatibleForReplacement(targetExpression, donorExpression)) {
            return false;
        }

        Expression donorClone = donorExpression.clone();
        invalidateRanges(donorClone);
        targetExpression.replace(donorClone);
        edits.add(edit);
        return true;
    }

    private boolean applyBinaryOperatorMutation(Edit edit) {
        Integer targetExpressionIndex = edit.targetExpressionIndex();
        Integer operatorCode = edit.donorExpressionIndex();
        if (targetExpressionIndex == null || operatorCode == null) {
            return false;
        }

        Statement targetStatement = getMutableStatementAt(edit.statementIndex());
        if (targetStatement == null) {
            return false;
        }

        List<Expression> targetExpressions = getReplaceableExpressions(targetStatement);
        if (targetExpressionIndex < 0 || targetExpressionIndex >= targetExpressions.size()) {
            return false;
        }

        Expression expression = targetExpressions.get(targetExpressionIndex);
        if (!expression.isBinaryExpr()) {
            return false;
        }

        BinaryExpr binaryExpr = expression.asBinaryExpr();
        BinaryExpr.Operator newOperator = fromBinaryOperatorCode(operatorCode);
        if (newOperator == null || binaryExpr.getOperator() == newOperator) {
            return false;
        }

        if (!candidateOperators(binaryExpr.getOperator()).contains(newOperator)) {
            return false;
        }

        binaryExpr.setOperator(newOperator);
        edits.add(edit);
        return true;
    }

    private boolean applyNegateExpression(Edit edit) {
        Integer targetExpressionIndex = edit.targetExpressionIndex();
        if (targetExpressionIndex == null) {
            return false;
        }

        Statement targetStatement = getMutableStatementAt(edit.statementIndex());
        if (targetStatement == null) {
            return false;
        }

        List<Expression> expressions = getNegatableExpressions(targetStatement);
        if (targetExpressionIndex < 0 || targetExpressionIndex >= expressions.size()) {
            return false;
        }

        Expression targetExpression = expressions.get(targetExpressionIndex);
        Expression replacement;

        if (targetExpression.isUnaryExpr() && targetExpression.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS) {
            replacement = targetExpression.asUnaryExpr().getExpression().clone();
        } else {
            replacement = new UnaryExpr(targetExpression.clone(), UnaryExpr.Operator.MINUS);
        }

        invalidateRanges(replacement);
        targetExpression.replace(replacement);
        edits.add(edit);
        return true;
    }


    private Integer selectDonorStatementIndex(int targetIndex, Edit.Type operation, Random random) {
        List<Statement> statements = getMutableStatements();
        if (statements.size() < 2) {
            return null;
        }

        if (operation == INSERT || operation == SWAP) {
            List<Integer> donors = new ArrayList<>();
            for (int i = 0; i < statements.size(); i++) {
                if (operation == SWAP && i == targetIndex) {
                    continue;
                }
                donors.add(i);
            }
            if (donors.isEmpty()) {
                return null;
            }
            return donors.get(random.nextInt(donors.size()));
        }

        Statement target = getMutableStatementAt(targetIndex);
        if (target == null) {
            return null;
        }

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            if (i == targetIndex && operation != REPLACE_EXPR) {
                continue;
            }

            Statement donor = statements.get(i);
            if (operation == SWAP && !target.getClass().equals(donor.getClass())) {
                continue;
            }
            if (operation == REPLACE_EXPR && getReplaceableExpressions(donor).isEmpty()) {
                continue;
            }
            candidates.add(i);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // For expression replacement we strongly prefer same statement kind, but keep fallback diversity.
        if (operation == REPLACE_EXPR && random.nextDouble() < 0.8) {
            List<Integer> sameCallable = new ArrayList<>();
            for (Integer candidate : candidates) {
                if (isSameEnclosingCallable(target, statements.get(candidate))) {
                    sameCallable.add(candidate);
                }
            }
            if (!sameCallable.isEmpty()) {
                candidates = sameCallable;
            }
        }

        if (operation == REPLACE_EXPR && random.nextDouble() < 0.7) {
            List<Integer> sameKind = new ArrayList<>();
            for (Integer candidate : candidates) {
                if (statements.get(candidate).getClass().equals(target.getClass())) {
                    sameKind.add(candidate);
                }
            }
            if (!sameKind.isEmpty()) {
                candidates = sameKind;
            }
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private Integer chooseWeightedIndex(List<Double> weights, Random random) {
        double total = 0.0;
        for (double weight : weights) {
            total += Math.max(0.0, weight);
        }
        if (total <= 0.0) {
            return null;
        }

        double pick = random.nextDouble() * total;
        double cumulative = 0.0;
        for (int i = 0; i < weights.size(); i++) {
            cumulative += Math.max(0.0, weights.get(i));
            if (pick <= cumulative) {
                return i;
            }
        }
        return weights.size() - 1;
    }

    private Integer currentStatementIndex(Statement statement) {
        List<Statement> currentStatements = getMutableStatements();
        for (int i = 0; i < currentStatements.size(); i++) {
            if (currentStatements.get(i) == statement) {
                return i;
            }
        }
        return null;
    }

    private boolean isMutableTargetIndex(int index) {
        Statement statement = getMutableStatementAt(index);
        if (statement == null) {
            return false;
        }
        return getSuspiciousness(statement) > 0.0;
    }

    private boolean computeHasExactPositiveStatementWeight() {
        if (suspiciousness == null || suspiciousness.isEmpty()) {
            return false;
        }
        for (Statement statement : getMutableStatements()) {
            if (getExactStatementSuspiciousness(statement) > 0.0) {
                return true;
            }
        }
        return false;
    }

    private double getExactStatementSuspiciousness(Statement statement) {
        if (statement.getRange().isPresent()) {
            Range range = statement.getRange().get();
            if (range.begin.line >= 0 && range.end.line >= range.begin.line) {
                boolean mappedLineSeen = false;
                double maxWeight = 0.0;
                for (int line = range.begin.line; line <= range.end.line; line++) {
                    Double lineWeight = suspiciousness.get(line);
                    if (lineWeight != null) {
                        mappedLineSeen = true;
                        maxWeight = Math.max(maxWeight, lineWeight);
                    }
                }
                if (mappedLineSeen) {
                    return maxWeight;
                }
            }
        }

        int beginLine = statement.getBegin().map(position -> position.line).orElse(-1);
        if (beginLine >= 0) {
            Double beginWeight = suspiciousness.get(beginLine);
            if (beginWeight != null) {
                return beginWeight;
            }
        }
        return UNKNOWN_SUSPICIOUSNESS;
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

    private Integer selectExpressionIndex(List<Expression> expressions, Random random, boolean weighted) {
        if (expressions.isEmpty()) {
            return null;
        }
        if (!weighted) {
            return random.nextInt(expressions.size());
        }

        List<Double> weights = new ArrayList<>(expressions.size());
        for (Expression expression : expressions) {
            weights.add(getExpressionPriority(expression));
        }
        Integer weightedIndex = chooseWeightedIndex(weights, random);
        return weightedIndex != null ? weightedIndex : random.nextInt(expressions.size());
    }

    private double getExpressionPriority(Expression expression) {
        if (expression.getParentNode().isPresent()
            && expression.getParentNode().get() instanceof com.github.javaparser.ast.expr.ConditionalExpr conditional
            && (conditional.getThenExpr() == expression || conditional.getElseExpr() == expression)) {
            return 3.6;
        }
        if (expression.isFieldAccessExpr()) {
            return 2.4;
        }
        if (expression.isArrayAccessExpr()) {
            return 2.5;
        }
        if (expression.isMethodCallExpr()) {
            return 2.6;
        }
        if (expression.isBinaryExpr()) {
            BinaryExpr binaryExpr = expression.asBinaryExpr();
            return switch (binaryExpr.getOperator()) {
                case LESS, LESS_EQUALS, GREATER, GREATER_EQUALS, EQUALS, NOT_EQUALS -> 2.7;
                default -> 2.8;
            };
        }
        return 1.0;
    }

    private boolean areCompatibleForReplacement(Expression targetExpression, Expression donorExpression) {
        // Prefer same AST kinds; only allow a few broad but safe cross-kind replacements.
        if (targetExpression.getClass().equals(donorExpression.getClass())) {
            return true;
        }

        if (targetExpression.isBinaryExpr() && donorExpression.isMethodCallExpr()) {
            return true;
        }
        if (targetExpression.isMethodCallExpr() && donorExpression.isFieldAccessExpr()) {
            return true;
        }
        if (targetExpression.isFieldAccessExpr() && donorExpression.isMethodCallExpr()) {
            return true;
        }

        if (isBooleanConditionExpression(targetExpression) && !isLikelyBoolean(donorExpression)) {
            return false;
        }
        if (!isBooleanConditionExpression(targetExpression) && isLikelyBoolean(donorExpression) && !isLikelyBoolean(targetExpression)) {
            return false;
        }

        return true;
    }

    private boolean isBooleanConditionExpression(Expression expression) {
        if (expression.getParentNode().isEmpty()) {
            return false;
        }
        Node parent = expression.getParentNode().get();

        if (parent instanceof com.github.javaparser.ast.stmt.IfStmt ifStmt) {
            return ifStmt.getCondition() == expression;
        }
        if (parent instanceof com.github.javaparser.ast.stmt.WhileStmt whileStmt) {
            return whileStmt.getCondition() == expression;
        }
        if (parent instanceof com.github.javaparser.ast.stmt.DoStmt doStmt) {
            return doStmt.getCondition() == expression;
        }
        if (parent instanceof com.github.javaparser.ast.stmt.ForStmt forStmt) {
            return forStmt.getCompare().map(compare -> compare == expression).orElse(false);
        }
        if (parent instanceof com.github.javaparser.ast.expr.ConditionalExpr conditionalExpr) {
            return conditionalExpr.getCondition() == expression;
        }
        return false;
    }

    private boolean isLikelyBoolean(Expression expression) {
        if (expression.isBooleanLiteralExpr()) {
            return true;
        }
        if (expression.isUnaryExpr() && expression.asUnaryExpr().getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            return true;
        }
        if (expression.isBinaryExpr()) {
            BinaryExpr.Operator operator = expression.asBinaryExpr().getOperator();
            return switch (operator) {
                case OR, AND, EQUALS, NOT_EQUALS, LESS, LESS_EQUALS, GREATER, GREATER_EQUALS -> true;
                default -> false;
            };
        }
        return expression.isInstanceOfExpr();
    }

    private boolean isSameEnclosingCallable(Statement left, Statement right) {
        var leftCallable = left.findAncestor(CallableDeclaration.class).orElse(null);
        var rightCallable = right.findAncestor(CallableDeclaration.class).orElse(null);
        return leftCallable != null && leftCallable == rightCallable;
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

    private Integer pickDifferentExpressionIndex(int size, int excludedIndex, Random random) {
        if (size <= 1) {
            return null;
        }

        int candidate = random.nextInt(size - 1);
        if (candidate >= excludedIndex) {
            candidate++;
        }
        return candidate;
    }

    private int binaryOperatorCode(BinaryExpr.Operator operator) {
        return switch (operator) {
            case LESS -> 1;
            case LESS_EQUALS -> 2;
            case GREATER -> 3;
            case GREATER_EQUALS -> 4;
            case EQUALS -> 5;
            case NOT_EQUALS -> 6;
            case PLUS -> 7;
            case MINUS -> 8;
            default -> 0;
        };
    }

    private BinaryExpr.Operator fromBinaryOperatorCode(int code) {
        return switch (code) {
            case 1 -> BinaryExpr.Operator.LESS;
            case 2 -> BinaryExpr.Operator.LESS_EQUALS;
            case 3 -> BinaryExpr.Operator.GREATER;
            case 4 -> BinaryExpr.Operator.GREATER_EQUALS;
            case 5 -> BinaryExpr.Operator.EQUALS;
            case 6 -> BinaryExpr.Operator.NOT_EQUALS;
            case 7 -> BinaryExpr.Operator.PLUS;
            case 8 -> BinaryExpr.Operator.MINUS;
            default -> null;
        };
    }

    private void invalidateRanges(Node node) {
        node.walk(n -> n.setRange(INVALID_RANGE));
    }

    private Statement getMutableStatementAt(int index) {
        List<Statement> statements = getMutableStatements();
        if (index < 0 || index >= statements.size()) {
            return null;
        }
        return statements.get(index);
    }

    private List<Statement> getMutableStatements() {
        // Block statements are not directly mutated/swapped in this representation.
        return compilationUnit.findAll(Statement.class).stream()
            .filter(statement -> !statement.isBlockStmt())
            .toList();
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Patch with ").append(edits.size()).append(" edits:\n");
        for (Edit edit : edits) {
            sb.append(edit).append("\n");
        }
        return sb.toString();
    }
}
