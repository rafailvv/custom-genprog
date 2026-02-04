package edu.passau.apr.model;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
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
    private final CompilationUnit compilationUnit; // cache of the patched compilation unit
    private final Map<Statement, Double> statementSus; // map of suspicious values for nodes
    private final List<Edit> edits = new ArrayList<>(); // list of edits applied one after another

    public Patch(CompilationUnit compilationUnit, Map<Statement, Double> statementSus) {
        this.compilationUnit = compilationUnit.clone();
        this.statementSus = statementSus;
    }

    public Patch(String source, List<StatementWeight> nodeWeights) {
        this.statementSus = new HashMap<>();
        this.compilationUnit = StaticJavaParser.parse(source);
        this.compilationUnit.findAll(Statement.class).forEach(statement -> {
            int line = statement.getBegin().map(p -> p.line).orElse(-1);
            double weight = 0.0;
            for (var sw : nodeWeights) {
                if (sw.getLineNumber() == line) {
                    weight = sw.getWeight();
                    break;
                }
            }
            statementSus.put(statement, weight);
        });
    }

    public void doMutations(double mutationRate, Random random) {
        int i = 0;
        for (Statement stmt : compilationUnit.findAll(Statement.class)) {

            // skip block statements for insertions as parent might not be a block statement
            // TODO how should we handle this properly?
            if (stmt.isBlockStmt()) {
                i++; continue;
            }

            Double weight = statementSus.getOrDefault(stmt, 0.1); // assume 0.1 for modified statements
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
                                parent.getStatements().addBefore(donorStmt.first(), stmt);
                                edits.add(new Edit(INSERT, _i, donorStmt.second()));
                            });
                        }
                    }
                    case SWAP -> {
                        var donorStmt = getDonorStatement(random);
                        if (donorStmt != null) {
                            stmt.replace(donorStmt.first().clone());
                            donorStmt.first().replace(stmt.clone());
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
        List<Statement> statements = compilationUnit.findAll(Statement.class);
        if (statements.isEmpty()) {
            return null;
        }
        int index = random.nextInt(statements.size());
        return new Pair<>(statements.get(index), index);
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
