package edu.passau.apr.model;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import edu.passau.apr.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static edu.passau.apr.model.Edit.Type.DELETE;
import static edu.passau.apr.model.Edit.Type.INSERT;
import static edu.passau.apr.model.Edit.Type.SWAP;

/**
 * Represents a patch as a collection of edits.
 * A patch is applied to the buggy source code to create a candidate fix.
 */
public class Patch {
    private static final Range INVALID_RANGE = new Range(new Position(-1, -1), new Position(-1, -1));

    private final CompilationUnit compilationUnit; // cache of the patched compilation unit
    private final Map<Integer, Double> suspiciousness; // map of suspicious values for nodes (cache for faster access)
    private final List<Edit> edits = new ArrayList<>(); // list of edits applied one after another

    public Patch(CompilationUnit cu, Map<Integer, Double> nodeWeights) {
        this.compilationUnit = cu;
        this.suspiciousness = nodeWeights;
    }

    public Patch(String source, Map<Integer, Double> nodeWeights) {
        this.compilationUnit = StaticJavaParser.parse(source);
        this.suspiciousness = nodeWeights;
    }

    public void doMutations(double mutationRate, Random random) {
        int i = 0;
        for (Statement stmt : compilationUnit.findAll(Statement.class)) {

            // skip block statements as
            // - insertions are not possible as the parent might not be a block statement
            // - swaps might only be possible with other block statements
            if (stmt.isBlockStmt()) {
                i++; continue;
            }

            // Get suspiciousness weight at the line number of the statement.
            // For multiline statements, the first line is chosen as this should have the highest suspiciousness value and
            // is definitely run.
            var line = stmt.getBegin().map(p -> p.line).orElse(-1);
            var weight = suspiciousness.getOrDefault(line, 0.1); // assume 0.1 for modified statements

            if (random.nextDouble() < mutationRate && random.nextDouble() < weight) {
                var operation = choose(random, Edit.Type.values());
                switch (operation) {
                    case DELETE -> {
                        stmt.remove();
                        edits.add(new Edit(DELETE, i, null));
                    }
                    case INSERT -> {
                        var donorStmt = getDonorStatement(random);
                        if (donorStmt != null) {
                            int _i = i;
                            stmt.getParentNode().map(n -> (BlockStmt) n).ifPresent(parent -> {
                                var donorStmtClone = donorStmt.first().clone();
                                // invalidate range as the suspiciousness value is unknown
                                donorStmtClone.setRange(INVALID_RANGE);

                                parent.getStatements().addBefore(donorStmtClone, stmt);

                                edits.add(new Edit(INSERT, _i, donorStmt.second()));
                            });
                        }
                    }
                    case SWAP -> {
                        var donorStmt = getDonorStatement(random);
                        if (donorStmt != null) {
                            var a = donorStmt.first().clone();
                            var b = stmt.clone();

                            // Invalidate ranges so we do not find any suspiciousness values for them.
                            // This is required as cloned and modified statements still equal each other, so
                            // we need to query the suspiciousness values by line numbers each time.
                            a.setRange(INVALID_RANGE);
                            b.setRange(INVALID_RANGE);

                            stmt.replace(a);
                            donorStmt.first().replace(b);
                            edits.add(new Edit(SWAP, i, donorStmt.second()));
                        }
                    }
                }
            }

            i++;
        }
    }

    private Edit.Type choose(Random random, Edit.Type[] values) {
        int index = random.nextInt(values.length);
        return values[index];
    }


    private Pair<Statement, Integer> getDonorStatement(Random random) {
        // do not use block statements as donors as they cannot be swapped with other statements, if they are part of e.g. an if statement
        // it also helps to produce smaller patches
        List<Statement> statements = compilationUnit.findAll(Statement.class).stream().filter(s -> !s.isBlockStmt()).toList();
        if (statements.isEmpty()) {
            return null;
        }
        int index = random.nextInt(statements.size());
        var donorStmt = statements.get(index);
        return new Pair<>(donorStmt, index);
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


    public boolean applyEdit(Edit edit) {
        try {
            return switch (edit.type()) {
                case DELETE -> applyDelete(edit);
                case INSERT -> applyInsert(edit);
                case SWAP -> applySwap(edit);
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Patch(compilationUnit.clone(), suspiciousness);
    }
}
